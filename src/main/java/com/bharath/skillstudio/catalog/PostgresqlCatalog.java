package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class PostgresqlCatalog {

    private PostgresqlCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("postgresql", "PostgreSQL",
                "Postgres is the system of record. Indexes, transactions, MVCC, and constraints are the ideas that pay the rent.",
                Concepts.of("Indexes you can explain",
                                "If EXPLAIN ANALYZE does not use your index, it is a comment. Match the WHERE and the sort.",
                                "Search by company and recency seq-scanned 200k portal rows until a btree on (company, posted_at DESC) matched the query.")
                        .depth("""
                                A btree index is an ordered map. It helps equality, range, and ORDER BY that match the column order. Left-prefix matters: (company, posted_at) helps WHERE company = ? ORDER BY posted_at DESC. It does not help WHERE posted_at > ? alone.

                                Partial indexes (WHERE status = 'queued') stay small. Covering indexes (INCLUDE) avoid heap fetches when you only need those columns. Too many indexes slow writes. Unused indexes are dead weight — pg_stat_user_indexes.

                                In interviews, write the index that matches a given WHERE+ORDER BY, and say how you would prove it.
                                """)
                        .qa("Why didn't Postgres use my index?",
                                "The predicate does not match (function on the column, wrong order, low selectivity). EXPLAIN ANALYZE is the proof, not the CREATE INDEX statement.",
                                "What is a left-prefix rule?")
                        .qa("When is a partial index worth it?",
                                "When almost all queries filter a common slice (status = queued) and the rest of the table would bloat the index.",
                                "Do indexes slow writes?")
                        .sample("Match the query",
                                """
                                        CREATE INDEX ON job_posting (company, posted_at DESC);
                                        """,
                                "For WHERE company = ? ORDER BY posted_at DESC."),
                Concepts.of("Transactions and isolation",
                                "Read committed is the default. Know when you need repeatable read, serializable, or SELECT FOR UPDATE.",
                                "Two workers both saw status=queued and sent the same apply email until the claim used SELECT FOR UPDATE SKIP LOCKED.")
                        .depth("""
                                A transaction is all-or-nothing for the writes you made. Isolation decides what you see of others. Read committed: each statement sees the latest committed rows. Repeatable read in Postgres is snapshot-based: you see a frozen snapshot. Serializable adds conflict detection.

                                SELECT FOR UPDATE locks the row. SKIP LOCKED lets workers race without waiting — a queue pattern. Holding a transaction open while you call HTTP is how you bloat MVCC and stall others.

                                In interviews, pick the isolation, then the lock, then why HTTP does not belong inside the transaction.
                                """)
                        .qa("How do two workers avoid sending the same email?",
                                "Claim with UPDATE ... WHERE id = ? AND status = 'queued' RETURNING *, or SELECT FOR UPDATE SKIP LOCKED. Unique constraints as a backstop.",
                                "Why not SERIALIZABLE for everything?")
                        .qa("Can you HTTP-call inside a transaction?",
                                "You can, and you will hold row versions and locks for the round trip. Do the work, commit, then call, or use an outbox.",
                                "What does SKIP LOCKED do?")
                        .sample("Claim a row",
                                """
                                        UPDATE apply_job
                                           SET status = 'sending'
                                         WHERE id = (
                                            SELECT id FROM apply_job
                                             WHERE status = 'queued'
                                             ORDER BY id
                                             FOR UPDATE SKIP LOCKED
                                             LIMIT 1
                                         )
                                         RETURNING *;
                                        """,
                                "Workers do not wait on each other."),
                Concepts.of("MVCC and bloat",
                                "Updates make new row versions. VACUUM is not optional. Long transactions freeze cleanup and look like 'disk is full'.",
                                "A tracker table that updated status all day died without vacuum. The incident looked like disk, the cause was a dashboard that held an idle-in-transaction session.")
                        .depth("""
                                Postgres does not overwrite rows in place. Dead versions pile up until VACUUM (usually autovacuum) reclaims them. A session that sits in a transaction prevents cleanup of versions newer than its snapshot. replication slots and prepared transactions can do the same.

                                HOT updates (same page, indexed columns unchanged) are cheaper. Updating an indexed column is a delete plus insert in the index. Fillfactor and bloat monitoring are ops, but engineers cause bloat with chatty updates and open transactions.

                                In interviews, connect UPDATE, dead tuples, autovacuum, and idle-in-transaction.
                                """)
                        .qa("Why did the table grow after constant status updates?",
                                "Each UPDATE makes a new version. VACUUM was blocked or too slow. Check idle-in-transaction and autovacuum stats.",
                                "What is a dead tuple?")
                        .qa("Is VACUUM FULL the first fix?",
                                "No. That rewrites the table and locks. Fix the long transaction and autovacuum first. FULL is a last resort.",
                                "How do indexed columns change the cost of UPDATE?")
                        .sample("Find blockers",
                                """
                                        SELECT pid, state, query, xact_start
                                          FROM pg_stat_activity
                                         WHERE xact_start IS NOT NULL
                                         ORDER BY xact_start;
                                        """,
                                "Idle-in-transaction is the usual villain. Screen only."),
                Concepts.of("JSONB with a schema story",
                                "JSONB is great for optional payload. It is not a substitute for columns you filter on every time.",
                                "Portal raw JSON lived in jsonb. Match score stayed a real column so search could index it.")
                        .depth("""
                                jsonb is binary JSON with operators and GIN indexes. jsonb_path_ops GIN helps containment. Expression indexes help a hot key. If you always filter on status, make a column (or a generated column) and a btree.

                                jsonb is not schemaless freedom. You still have a schema — it is just unenforced until you add check constraints or validate in the app. Document which keys exist.

                                In interviews, 'payload plus generated column' is the grown-up answer.
                                """)
                        .qa("Why not put everything in jsonb?",
                                "Filters, sorts, foreign keys, and constraints want columns. jsonb is for the tail of the payload and vendor blobs.",
                                "When is a GIN index worth it?")
                        .qa("How do you promote a hot key?",
                                "A generated column or a real column plus a backfill. Index that. Leave the rest in jsonb.",
                                "Does jsonb replace migrations?")
                        .sample("Split the payload",
                                """
                                        payload jsonb,
                                        status text GENERATED ALWAYS AS (payload->>'status') STORED
                                        """,
                                "Status can be indexed. The blob stays."),
                Concepts.of("Migrations as code",
                                "Flyway or Liquibase in the same repo as the app. Expand-contract, never edit a shipped version.",
                                "Adding a table in V2 without rewriting V1 let existing databases keep booting.")
                        .depth("""
                                A migration is an immutable artifact. Once it ran in prod, you do not edit it. Fix-forward with V3. Expand-contract: add the new column, deploy readers that tolerate both, backfill, switch writers, drop the old column later.

                                Locks matter. CREATE INDEX CONCURRENTLY cannot run in a transaction in Postgres. Adding a column with a default on older versions rewrote the table — know your version. Expand-contract exists because deploys are rolling.

                                In interviews, walk a rename without downtime.
                                """)
                        .qa("Can you edit V2 after it shipped?",
                                "No. New files only. Checksums will fail on environments that already applied V2.",
                                "What is expand-contract?")
                        .qa("Why is a default on a new column dangerous?",
                                "On older Postgres it rewrote every row. Even now, think about locks and backfill. Prefer nullable add, then backfill, then constrain.",
                                "How do you index without locking writes?")
                        .sample("Forward only",
                                """
                                        -- V3__add_match_reason.sql
                                        ALTER TABLE job_posting ADD COLUMN match_reason text;
                                        """,
                                "V2 stays untouched."),
                Concepts.of("Constraints as the last schema",
                                "Unique, foreign key, not null, and check are the database's opinion. The app will miss a path. The constraint will not.",
                                "A unique (job_id, channel) stopped a double apply that the service missed when two tabs raced.")
                        .depth("""
                                Constraints are concurrent-safe in a way your Java if-statement is not. Unique violations become 409 at the API. Foreign keys stop orphan scores. Check constraints catch money < 0 that skipped the record constructor.

                                Deferrable constraints exist; most apps do not need them. Disabling constraints to 'speed a load' is how you load garbage.

                                In interviews, say which uniqueness is the product rule and that the database enforces it.
                                """)
                        .qa("Is a unique constraint enough for idempotency?",
                                "It is the backstop. Catch the violation and return the existing row. The app still tries the insert.",
                                "When do you use a partial unique index?")
                        .qa("Why not only validate in Java?",
                                "Two workers, a forgotten endpoint, a one-off script. The database is the last schema.",
                                "What HTTP status for a unique violation?")
                        .sample("Race-safe uniqueness",
                                """
                                        CREATE UNIQUE INDEX ON apply_job (job_id, channel);
                                        """,
                                "Two tabs cannot both succeed."),
                Concepts.of("Connection pooling",
                                "The pool is the real bulkhead. Size it for Postgres, not for your thread count. A leak looks like 'DB is slow'.",
                                "Virtual threads plus a pool of 20 meant 20 queries at a time. Raising the pool to match thread count knocked over the database.")
                        .depth("""
                                HikariCP is Boot's default. max pool size times replica count is the load on Postgres max_connections. Leave headroom for admin and migrations. Connection timeout is how you fail fast; a five-minute wait is a hanging API.

                                Leaks: a connection borrowed and never returned, often from skipping try-with-resources or holding it across HTTP. Leak detection in Hikari is a test setting you should try.

                                In interviews, size the pool from the database, not from virtual thread enthusiasm.
                                """)
                        .qa("Why not one connection per virtual thread?",
                                "Postgres cannot take it. The pool is the bulkhead. Excess work waits on the pool, which is correct.",
                                "What is a connection leak?")
                        .qa("How do you pick max pool size?",
                                "Measure active queries, leave room for other services sharing the instance, stay under max_connections. Start small.",
                                "Where should leakDetectionThreshold live?")
                        .sample("Hikari bound",
                                """
                                        spring.datasource.hikari.maximum-pool-size=20
                                        spring.datasource.hikari.connection-timeout=3000
                                        """,
                                "Fail fast. Do not match thread count."),
                Concepts.of("EXPLAIN ANALYZE as a habit",
                                "Plans are the review artifact for a slow query. Guessing an index without a plan is cargo cult.",
                                "A 'missing index' was a sequential scan because the predicate wrapped the column in LOWER. A functional index or a citext column matched the plan.")
                        .depth("""
                                EXPLAIN shows the planner's guess. EXPLAIN ANALYZE runs it and shows actual rows and time. Nested loops versus hash joins, seq scan versus index scan, and row-estimate errors are the vocabulary.

                                ANALYZE (the command) updates statistics. A fresh table with no stats will pick a stupid plan. Work_mem, random_page_cost, and parameterized plans matter in production; do not cargo-cult SET flags blindly.

                                In interviews, read a simple plan out loud: seq scan on a 200k table, then the index you would add, then ANALYZE again.
                                """)
                        .qa("EXPLAIN versus EXPLAIN ANALYZE?",
                                "EXPLAIN is the guess. ANALYZE executes and shows actuals. Use ANALYZE on a copy or a safe query — it does the work.",
                                "Why did the planner seq-scan a selective column?")
                        .qa("A function on the column in WHERE. What happens?",
                                "The plain btree may not apply. Use a functional index, or store a normalized column.",
                                "When do you run ANALYZE?")
                        .sample("Prove it",
                                """
                                        EXPLAIN ANALYZE
                                        SELECT * FROM job_posting
                                         WHERE company = 'Acme'
                                         ORDER BY posted_at DESC
                                         LIMIT 20;
                                        """,
                                "Look for Index Scan. Screen only.")
        );
    }
}
