package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class RedisCatalog {

    private RedisCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("redis", "Redis",
                "Redis is a data structure server. Use it as a cache, a lock, or a queue with eyes open about durability.",
                Concepts.of("Cache with a TTL",
                                "A cache without TTL is a second database you forgot to expire. Key names include the version of the value shape.",
                                "Adzuna results cached for a few minutes so a refresh did not burn the quota. A deploy that changed JSON shape used job:v2: as the prefix so old blobs died.")
                        .depth("""
                                Every cache key needs a TTL unless you have a hard invalidation path you have tested. Memory is not free; Redis will evict (or OOM) according to maxmemory-policy. Version the key when the value schema changes so you do not deserialize yesterday's JSON.

                                Stampede: many requests miss together and hit the DB. Soft TTL plus singleflight (or SET NX lock around the load) is the usual fix.

                                In interviews, TTL, key version, stampede.
                                """)
                        .qa("What happens if you never set TTL?",
                                "The key lives until eviction or a flush. You built a second, worse database. Always expire or have a proven delete path.",
                                "How do you change the JSON shape?")
                        .qa("Cache stampede?",
                                "A hot key expires, N threads load the DB. Use a lock around rebuild, or probabilistic early expiration.",
                                "Where do you store the TTL policy?")
                        .sample("SET with expiry",
                                """
                                        redis.setex("job:v2:" + id, 300, json);
                                        """,
                                "Five minutes. Shape version in the key."),
                Concepts.of("Aside versus write-through",
                                "Cache-aside is the default: miss, load, set. Write-through is for when stale reads hurt more than extra writes.",
                                "A skills-gap result could be aside. A live apply status should not sit stale in Redis while Postgres moved on.")
                        .depth("""
                                Aside: app reads Redis, on miss reads DB, then SET. Writers update DB and delete (or update) the key. Forgot delete is stale-forever if TTL is long.

                                Write-through: writers always update cache and DB. More write load, less stale. Write-behind is a queue to the DB — durability risk.

                                In interviews, pick aside for read-heavy computable data, and say how you invalidate.
                                """)
                        .qa("Who deletes the key on write?",
                                "The writer. If two services write, they both must, or TTL is your only safety.",
                                "When is write-through worth it?")
                        .qa("Is Redis the source of truth for apply status?",
                                "No. Postgres is. Redis can hint. If Redis dies, you rebuild from the DB.",
                                "What is write-behind's failure?")
                        .sample("Aside",
                                """
                                        String json = redis.get(key);
                                        if (json == null) {
                                            json = db.load(id);
                                            redis.setex(key, 300, json);
                                        }
                                        """,
                                "Delete the key on update."),
                Concepts.of("Locks",
                                "SET NX EX is a lock. You must expire it. You must not hold it across a user click.",
                                "Two tabs must not enqueue the same job twice. A 30-second lock around the insert, plus a unique constraint, stopped it.")
                        .depth("""
                                SET key value NX EX seconds is the primitive. The value should be a unique token so you only delete your own lock (check-and-delete, ideally a Lua script). Forgetting EX is a forever lock when the process dies.

                                Redlock is controversial for correctness on a single instance; for a job enqueue, NX EX plus a unique constraint is enough. Do not hold a lock while waiting for a human.

                                In interviews, NX EX, token, and the DB unique backstop.
                                """)
                        .qa("Process dies while holding the lock?",
                                "TTL frees it. Without TTL, you wait for an operator. Always EX.",
                                "Why a random token as the value?")
                        .qa("Is Redis lock enough without a unique constraint?",
                                "No. Two app instances, a lock bug, a replay. The database unique is the last schema.",
                                "Can you lock across a request to the user?")
                        .sample("Short lease",
                                """
                                        Boolean ok = redis.set("lock:apply:" + jobId, token, "NX", "EX", 30);
                                        """,
                                "Then insert. Then delete if value still token."),
                Concepts.of("Streams versus lists",
                                "Lists are a queue. Streams give you consumer groups and a pending list. Pick one and document why.",
                                "Inbound recruiter mail that several workers share is a stream, not a single LPUSH that one worker steals blindly without ack.")
                        .depth("""
                                List: LPUSH/BRPOP. Simple, lost on crash if you already popped. Streams: XADD, consumer groups, XACK, pending entries. You can claim stuck pending messages. Pub/sub is fire-and-forget fanout, not a queue — if nobody listens, the message is gone.

                                Use Kafka when you need a durable multi-day log and replay. Redis streams are a fast buffer with a Redis durability dial.

                                In interviews, list versus stream versus pub/sub versus Kafka.
                                """)
                        .qa("Why not pub/sub for work items?",
                                "If the worker is down, the message is gone. Pub/sub is fanout for live listeners, not a job queue.",
                                "What is XACK?")
                        .qa("When do you pick Kafka over Redis streams?",
                                "Long retention, many independent consumer groups, replay a day later, operational isolation from the cache cluster.",
                                "What is a pending entry?")
                        .sample("Stream add",
                                """
                                        XADD mail:in * from recruiter@acme subject "Staff Java"
                                        """,
                                "Workers in a group XREADGROUP and XACK."),
                Concepts.of("Durability is a dial",
                                "AOF and RDB are optional. If the data cannot be rebuilt, Redis is the wrong store.",
                                "Tracker state belongs in Postgres. Redis can die. Cache keys rebuild. Locks expire.")
                        .depth("""
                                RDB snapshots, AOF fsync policies (always, every second, never), replication. A restart can lose the last second on common configs. That is fine for a cache. It is not fine for money.

                                Redis Cluster and Sentinel are availability, not a substitute for 'this must not vanish'. Persistence plus backups if you insist on Redis as a primary — most Java backends should not.

                                In interviews, rebuildability is the test.
                                """)
                        .qa("appendonly no — is that OK?",
                                "For a pure cache, yes. For a queue of irreplaceable events, no.",
                                "What does everysec AOF miss?")
                        .qa("Can Redis replace Postgres for applications?",
                                "Not as the system of record for this domain. Constraints, transactions across rows, and reporting still want SQL.",
                                "What do you do when Redis is empty at boot?")
                        .sample("Pure cache",
                                """
                                        appendonly no
                                        maxmemory-policy allkeys-lru
                                        """,
                                "Expect emptiness. Rebuild from DB."),
                Concepts.of("Key naming",
                                "A key is an API. Namespaces, versions, and ids. Collisions are silent data corruption.",
                                "job:123 from two features collided. job:v2:match:123 and apply:lock:123 did not.")
                        .depth("""
                                Pattern: app:domain:version:id. Use HASH for several fields of one entity if you always fetch them together. SCAN not KEYS in prod. Avoid huge key names; avoid unbounded id in a tag that creates millions of unique keys without TTL.

                                A shared Redis with no prefix is how staging flushes prod. Separate DBs (numeric) are weak isolation; prefer separate instances for prod.

                                In interviews, a naming scheme and KEYS versus SCAN.
                                """)
                        .qa("Why not KEYS *?",
                                "It blocks the single-threaded server on a large keyspace. SCAN is incremental.",
                                "When is a HASH better than many string keys?")
                        .qa("Staging and prod on one Redis?",
                                "Don't. Prefixes fail eventually. Separate instances.",
                                "What goes in the key versus the value?")
                        .sample("Versioned names",
                                """
                                        job:v2:match:123
                                        apply:lock:123
                                        """,
                                "Feature, version, id."),
                Concepts.of("Eviction policies",
                                "When maxmemory hits, Redis evicts or refuses writes. allkeys-lru is a cache. noeviction is a database that will error.",
                                "A lock key got evicted under allkeys-lru because it had no TTL and lost the policy lottery. volatile-lru plus TTLs on cache keys protected the locks — better: separate logical uses.")
                        .depth("""
                                noeviction: writes fail when full. allkeys-lru/lfu: any key can go. volatile-*: only keys with TTL. Choose for the workload. Mixing cache and locks on one instance with allkeys-lru will drop locks.

                                Monitor used_memory and evicted_keys. A sudden evict storm is a capacity incident.

                                In interviews, policy plus why mixed workloads need isolation.
                                """)
                        .qa("allkeys-lru on a lock key without TTL?",
                                "The lock can vanish. volatile-lru would spare it — and then you can OOM. Prefer TTL on locks and a policy you can explain.",
                                "What does noeviction do to SET?")
                        .qa("How do you know eviction is happening?",
                                "INFO stats evicted_keys, metrics, and a memory graph. Not a user report of 'sometimes the cache is empty' only.",
                                "LFU versus LRU?")
                        .sample("Cache policy",
                                """
                                        maxmemory 256mb
                                        maxmemory-policy allkeys-lru
                                        """,
                                "Locks should not live here, or they need TTL."),
                Concepts.of("Pub/sub versus streams",
                                "Pub/sub is live fanout. It is not durable. If you need 'the other service will get this even if it was down', you wanted a stream or Kafka.",
                                "A 'user is typing' signal is pub/sub. An 'application submitted' event is not.")
                        .depth("""
                                SUBSCRIBE/PUBLISH deliver to current subscribers. No backlog. No ack. Connection drops lose the message. Streams and Kafka keep a log.

                                Redis pub/sub is fine for cache invalidation hints ('drop key X') if a missed message only means stale until TTL. It is not fine for billing events.

                                In interviews, a yes-example and a no-example.
                                """)
                        .qa("Cache invalidation over pub/sub?",
                                "Acceptable if TTL bounds staleness. A missed message means a key lives until expiry.",
                                "Why not billing on pub/sub?")
                        .qa("How do subscribers scale?",
                                "Each subscriber gets a copy. That is fanout, not work-sharing. Work-sharing is a stream group or Kafka group.",
                                "What happens on reconnect?")
                        .sample("Invalidate hint",
                                """
                                        PUBLISH cache:invalidate job:v2:match:123
                                        """,
                                "Missed publish is OK; TTL still dies.")
        );
    }
}
