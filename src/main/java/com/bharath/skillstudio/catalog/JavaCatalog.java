package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class JavaCatalog {

    private JavaCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("java", "Java / OpenJDK",
                "This huddle is the Java you actually ship: identity, collections, exceptions, concurrency, cheaper blocking I/O, honest time, and data carriers that fail at compile time.",
                Concepts.of("equals, hashCode, and identity",
                                "Collections, caches, and JPA entities all decide membership with equals and hashCode. Mix identity with value equality and you lose rows in a HashSet or leak entries in a HashMap.",
                                "A payment idempotency cache keyed by a Money-like class dropped duplicates because equals used == on a wrapped currency string, so two equal amounts became two keys and the second charge went through.")
                        .depth("""
                                Java has two notions of sameness. == is identity: the same object in memory. equals is value: the same business meaning. hashCode is the lookup contract that lets HashMap find that value in a bucket. The rule is mechanical and interviewers expect it verbatim: if a.equals(b) then a.hashCode() must equal b.hashCode(). The converse is not required, but collisions cost you CPU. If you override one, you override both, and you include exactly the fields that define equality — no timestamps you did not mean, no mutable counters.

                                Identity versus value is the fork that separates DTOs from entities. Records give you value equality for free; that is why they are right for Money and JobId. A JPA entity with a generated id is a different story. Before persist the id is null, so id-only equals makes every new Order look the same. After persist, business-field equality that includes a lastModified column will break a HashSet the moment you flush. Pick one model and write the test: two instances in a HashSet, then a HashMap get. toString is not proof.

                                Never put a mutable field into equals or hashCode if the object will live in a map. Changing the field after insert leaves the entry stranded in the old bucket. Prefer final components on value types. For Hibernate, equals on the identifier plus a type check that survives a proxy — instanceof, not getClass() if you use lazy loading — and do not hash an uninitialized lazy field. Lombok's @EqualsAndHashCode on an entity is how production maps silently rot.
                                """)
                        .qa("What is the equals and hashCode contract?",
                                "If two objects are equal they must share a hashCode. Hash-based collections use the hash to find a bucket and equals to confirm. Break one side and you get lost entries or duplicates.",
                                "Why must you not use a mutable field in hashCode?")
                        .qa("When should an entity use id equality versus a business key?",
                                "After persist, the primary key is identity. Before persist, id is null — all new entities would collapse together. Use a business key that exists at creation, or keep identity equality and do not put transient entities in a HashSet.",
                                "What does Hibernate do with a proxy if you compare getClass()?")
                        .qa("Why are records right for Money and wrong for a JPA entity?",
                                "Records are transparent value types: every component participates. Money should compare amount and currency. A JPA entity has generated ids, lazy fields, and mutation. Value equality on every column will break collections.",
                                "How do you test that two Money instances work as HashMap keys?")
                        .sample("Value type that can sit in a HashMap",
                                """
                                        public record Money(long minor, String currency) {
                                            public Money {
                                                if (currency == null || currency.length() != 3) {
                                                    throw new IllegalArgumentException("currency");
                                                }
                                            }
                                        }
                                        """,
                                "Two Money(100, \"INR\") instances are equal and share a hash. Screen only.")
                        .sample("Do not mutate a key",
                                """
                                        Map<JobKey, Status> seen = new HashMap<>();
                                        JobKey key = new JobKey("acme", "staff-java");
                                        seen.put(key, Status.NEW);
                                        // key.setTitle("principal"); // lost entry — hash moved, equals did not
                                        """,
                                "If the key can change, it was not a key. Use a record."),
                Concepts.of("Collections that pay rent",
                                "Most production Java is HashMap, ArrayList, and ConcurrentHashMap. Know their contracts, fail-fast iterators, and what happens under concurrent write. Fancy collections are a footnote until these three are automatic.",
                                "A job-match cache used a plain HashMap from a servlet thread and a mail poller. The map corrupted under load; ConcurrentHashMap with a size bound and a TTL policy was the fix, not a bigger heap.")
                        .depth("""
                                ArrayList is a contiguous array that amortizes append. Indexing is O(1). Inserting at zero is a copy. LinkedList almost never pays rent in modern JVMs: pointer chasing destroys the cache, and you almost never need a deque of nodes. If you need a queue, ArrayDeque. If you need random access, ArrayList. Measure before you reach for a linked structure because an interview anecdote said so.

                                HashMap is an array of bins, treeified when a bin gets long, with a load factor that defaults to 0.75. Null keys are allowed (one). Iteration is fail-fast: a structural change from another thread is a ConcurrentModificationException, not a successful walk. That exception is a gift. Swallowing it, or iterating a live map from two schedulers, is how you ship heisenbugs. LinkedHashMap preserves insertion or access order and is the right LRU sketch when you accessOrder=true and override removeEldestEntry.

                                ConcurrentHashMap is not a synchronized HashMap. Compound actions (check then put) still race unless you use putIfAbsent, compute, or merge. It does not allow null keys or values — that is a deliberate contract so containsKey is not ambiguous. Size is an estimate under contention. For a bounded cache, CHM plus an eviction policy you own, or Caffeine; do not invent a third map. In interviews, say which collection, why, and what breaks under a second writer.
                                """)
                        .qa("HashMap versus ConcurrentHashMap — when do you switch?",
                                "The moment two threads mutate. Synchronizing the whole map serializes everything. CHM gives concurrent reads and compound methods. You still need compute for check-then-act.",
                                "Why does ConcurrentHashMap forbid null values?")
                        .qa("Why is LinkedList rarely the right List?",
                                "It is slow to scan, hostile to the CPU cache, and you already have ArrayDeque for queues. ArrayList plus an index, or a Deque, covers almost every production case.",
                                "When is a TreeMap the right Map?")
                        .sample("Compound action on ConcurrentHashMap",
                                """
                                        statuses.compute(jobId, (id, current) -> {
                                            if (current == Status.SENT) {
                                                return current;
                                            }
                                            return Status.QUEUED;
                                        });
                                        """,
                                "putIfAbsent and compute are the API. get plus put is a race."),
                Concepts.of("Exceptions and try-with-resources",
                                "Checked versus unchecked is a boundary decision. try-with-resources is how you close JDBC, streams, and HTTP bodies. Swallowing exceptions and leaking connections are the same outage with different stack traces.",
                                "A portal client caught IOException, logged at debug, and returned empty listings. The connection pool starved because the response body was never closed. try-with-resources on the Response made the incident obvious instead of silent.")
                        .depth("""
                                Exceptions are control flow that skipped the return path. Use them for the path you do not want callers to forget. Unchecked exceptions (RuntimeException) are for programmer errors and for failures the caller cannot reasonably handle — a missing clock, a broken invariant. Checked exceptions are a forcing function at an I/O boundary. Wrapping a checked SQLException in an unchecked DataAccessException is the Spring choice: the service layer does not pretend it can recover from a downed database.

                                try-with-resources implements AutoCloseable. All resources declared in the parentheses close in reverse order, even if the body throws. The close exception is added as suppressed if the body already failed — you see both, which is the point. A finally block that also throws will hide the original. Prefer the language feature. Never close in a catch that returns a default; you will forget a branch.

                                Logging and wrapping have a rule: wrap once, log once. catch (Exception e) { log; throw e; } duplicates the stack in every layer. catch and return Optional.empty() without a counter is how an outage looks like an empty search page. Translate at the boundary (SQL to your domain), include the cause, and let @ControllerAdvice turn it into a problem response. InterruptedException is special: restore the interrupt flag if you cannot stop.
                                """)
                        .qa("Why try-with-resources instead of close in finally?",
                                "It closes in reverse order, keeps suppressed exceptions, and cannot skip a branch. finally that throws hides the original failure.",
                                "What is a suppressed exception?")
                        .qa("When do you wrap a checked exception?",
                                "At a boundary where the caller cannot recover — JDBC, HTTP — wrap in an unchecked type that names the dependency. Do not wrap just to dodge a throws clause on a private method.",
                                "What do you do with InterruptedException?")
                        .sample("Close the HTTP body",
                                """
                                        try (Response response = client.newCall(request).execute();
                                             InputStream body = response.body().byteStream()) {
                                            return parser.read(body);
                                        }
                                        """,
                                "If parse throws, the response still closes. Screen only."),
                Concepts.of("Concurrency: happens-before, volatile, synchronized",
                                "Threads do not see each other's writes unless a happens-before edge exists. volatile is visibility and ordering for one variable. synchronized is mutual exclusion plus that edge. Guessing with Thread.sleep is not a memory model.",
                                "A feature flag written by an admin thread stayed false for servlet workers because the boolean was a plain field. volatile, or better a dedicated config bean published after construction, made the flip visible.")
                        .depth("""
                                The Java Memory Model defines when a write becomes visible. A happens-before edge is created by unlocking a monitor then locking the same monitor, by writing a volatile then reading it, by starting a thread, by joining, and by concurrent utilities (CountDownLatch, CHM operations, and so on). Without an edge, a reader can see stale values forever, or a torn long on a 32-bit mental model — on 64-bit Java, longs are typically atomic, but visibility is still not guaranteed.

                                volatile guarantees that a write is visible to a later read of that same variable and that surrounding loads and stores are not reordered across it. It is not atomic increment. volatile int count; count++ is still a race. AtomicInteger exists for that. volatile is the right tool for a published immutable config reference or a shutdown flag. It is the wrong tool for a multi-field invariant — two volatiles do not update together.

                                synchronized (and ReentrantLock) give you exclusion and happens-before. Keep the critical section tiny. Do not call blocking I/O inside synchronized — that is how you pin virtual threads and stall platform threads. Double-checked locking without volatile on the instance field is still wrong. Prefer java.util.concurrent, then a lock, then volatile, then nothing. In an interview, draw the edge: who writes, who reads, what establishes happens-before.
                                """)
                        .qa("Does volatile make increment atomic?",
                                "No. It makes the latest write visible. count++ is read-modify-write. Use AtomicInteger or a lock.",
                                "When is a volatile flag enough?")
                        .qa("What does happens-before actually buy you?",
                                "Visibility and ordering. After an unlock, the next lock on that monitor sees the writes. After a volatile write, a later volatile read of that field sees it. Without an edge, stale data is legal.",
                                "Why is Thread.sleep not a synchronization edge?")
                        .sample("Publish a config snapshot",
                                """
                                        final class Flags {
                                            private volatile Config snapshot = Config.defaults();

                                            void replace(Config next) {
                                                snapshot = Objects.requireNonNull(next);
                                            }

                                            Config current() {
                                                return snapshot;
                                            }
                                        }
                                        """,
                                "The reference is volatile. The Config object should be immutable."),
                Concepts.of("Virtual threads",
                                "Platform threads are scarce OS threads. Virtual threads are cheap tasks the JVM mounts on carriers. Blocking JDBC or HTTP parks the virtual thread, not the carrier — unless you pin it.",
                                "A Spring MVC service that calls three downstream HTTP APIs per request kept a blocking style with virtual threads instead of rewriting the stack in WebFlux.")
                        .depth("""
                                A virtual thread is a JVM-scheduled task. When it hits a blocking JDK call that the runtime understands, it unmounts from its carrier so the carrier can run someone else. You can have hundreds of thousands of waiting virtual threads and a handful of carriers. That is the point of Thread.startVirtualThread and of Boot's spring.threads.virtual.enabled. CPU-bound work does not get cheaper; you only bought wait-time.

                                Pinning is the trap. A virtual thread that blocks while holding a synchronized monitor stays glued to the carrier. The carrier cannot help anyone else. ReentrantLock usually unmounts; synchronized around JDBC or HTTP does not (until newer JDK work lands in your actual runtime — verify, do not assume). JFR has a virtual-thread pinning event. If you cannot find pinning, you do not yet run virtual threads in anger.

                                Virtual threads do not replace a connection pool. A million virtual threads and twenty Postgres connections is still twenty concurrent queries; the rest wait on the pool. Size pools for the database, timeouts for the remote, and structured concurrency (StructuredTaskScope in newer JDKs) when you fan out. WebFlux remains the tool when you already have a reactive pipeline and need backpressure through the whole stack. Do not mix both as a fashion statement.
                                """)
                        .qa("When would you pick virtual threads over WebFlux?",
                                "When the team is faster in blocking style and the bottleneck is I/O wait, not CPU. Keep Spring MVC and JDBC. WebFlux pays off when you already have a reactive stack and need backpressure end to end.",
                                "What is pinning, and how would you find it?")
                        .qa("Do virtual threads replace a connection pool?",
                                "No. You can have a million virtual threads and still only 20 database connections. The pool is the bulkhead. Size it for the database.",
                                "How would you prove a service is pinning in production?")
                        .sample("Fan-out without a custom pool",
                                """
                                        Thread.startVirtualThread(() -> {
                                            var profile = profiles.load(id);
                                            var jobs = jobs.findBySkill(profile.skill());
                                            view.render(profile, jobs);
                                        });
                                        """,
                                "Readable blocking calls. The carrier is not stuck in JDBC if you do not pin."),
                Concepts.of("java.time, not Date",
                                "Date and Calendar are mutable instants pretending to be calendars. Instant, OffsetDateTime, and an injected Clock are what production systems use. Store UTC, format in the user's zone at the edge.",
                                "An India hiring portal showing interview slots stored local times without a zone. DST and a UTC server made candidates show up an hour off. timestamptz plus Asia/Kolkata at render time fixed it.")
                        .depth("""
                                Instant is a point on the timeline. OffsetDateTime is that point plus an offset, which is what JSON and many APIs want. ZonedDateTime is for civil time: a zone with rules, including DST. LocalDateTime has no zone — it is a wall clock, and it is the wrong type to store for an instant that must be unique in the world. java.util.Date is a mutable Instant in disguise; Calendar is worse. Do not convert by concatenating strings.

                                Inject Clock. clock.instant() is testable; Instant.now() is not. Tests pass Clock.fixed in UTC and assert the formatted Asia/Kolkata string. Postgres timestamptz stores an instant. Convert to the user zone only when you render. If you store a local time without a zone, you have already lost twice a year.

                                Durations and Periods are not the same: Duration is time-based (hours, seconds), Period is date-based (days, months). Adding a month is a Period. Timeouts are Durations. Never sleep by polling Instant.now in a loop without a clock you own. In interviews, say what you store, what you serialize, and how a reminder at 9am local is tested.
                                """)
                        .qa("What do you store in Postgres for an interview slot?",
                                "timestamptz — an instant. Convert to Asia/Kolkata only when you render. A local time without a zone will lie on DST.",
                                "How do you test a reminder that fires at 9am local?")
                        .qa("Why inject Clock?",
                                "Instant.now() is untestable. A fixed Clock makes a deadline test deterministic.",
                                "OffsetDateTime versus ZonedDateTime?")
                        .sample("Clock at the edge",
                                """
                                        Instant deadline = clock.instant().plus(Duration.ofHours(24));
                                        OffsetDateTime shown = deadline.atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
                                        """,
                                "Tests pass Clock.fixed. Production uses Clock.systemUTC."),
                Concepts.of("Records as DTOs",
                                "Records are transparent carriers: constructor, accessors, equals, hashCode, toString. That is what you want on an API boundary. They are a poor domain entity if the object must mutate or hide invariants behind setters.",
                                "A payment API returned an amount as a mutable class. A missing currency field bound as null and reached the ledger. A record with a compact constructor rejected the payload at the edge.")
                        .depth("""
                                A record is a final class with private final components, canonical constructor, accessors named after the components, and value-based equals. Compact constructors are where validation lives: if the money is negative, throw before the object exists. That is cheaper than a Bean Validation annotation you forgot to trigger.

                                Jackson on current Spring Boot binds records via the canonical constructor. A new component is a breaking constructor change; additive optional fields are easier on a class or a builder. Teams often keep a record for the wire and a mutable type inside an aggregate. Do not make a JPA entity a record — identity, proxies, and mutation fight the model.

                                Records work as Map keys, as event payloads, as command objects. They do not work as a place to hide a lazily computed cache in a non-canonical field you mutate later. If you need a setter, you wanted a class. In an interview, say boundary versus interior, and mention compact constructors instead of waving at Lombok.
                                """)
                        .qa("Why not make every class a record?",
                                "Entities with identity, lazy relations, and lifecycle are not tuples. Records shine for Money, JobId, and event payloads. If you need a setter, you wanted a class.",
                                "How does a compact constructor help validation?")
                        .qa("How does a record change JSON evolution?",
                                "A new component is a breaking constructor change. Additive optional fields are easier on a class or builder. Some teams keep a record for the wire and a class inside.",
                                "What if Jackson cannot see the canonical constructor?")
                        .sample("Money at the boundary",
                                """
                                        public record Money(long minor, String currency) {
                                            public Money {
                                                if (minor < 0) {
                                                    throw new IllegalArgumentException("minor");
                                                }
                                                if (currency == null || currency.length() != 3) {
                                                    throw new IllegalArgumentException("currency");
                                                }
                                            }
                                        }
                                        """,
                                "Invalid money never enters the service."),
                Concepts.of("Streams grouping, not everything",
                                "groupingBy and partitioningBy replace nested maps. A twelve-step map-filter-flatMap chain is how people hide a null. Prefer a for-loop when you have side effects, checked exceptions, or an early exit.",
                                "A tracker dashboard that counted applications by status drifted because three nested loops disagreed. One groupingBy with counting() became the single source.")
                        .depth("""
                                Streams are a library for describing bulk transformations. They shine when the output type is obvious in one breath: a List, a Map of counts, a joined string. groupingBy(classifier) and groupingBy(classifier, downstream) are the collectors that pay rent. partitioningBy is groupingBy for a boolean. toMap needs a merge function the moment keys collide — omitting it is a production surprise.

                                A pipeline with side effects is a lie: forEach sending mail inside a stream hides failures and order. Parallel streams steal the common ForkJoinPool, which Tomcat and other work also need. Almost never call parallel() on a request thread to 'go faster'. Measure. Usually a better algorithm or a database GROUP BY is the real fix.

                                Checked exceptions do not flow through lambdas without wrapping. If you are wrapping every line, write a loop. Debugging a breakpoint in the middle of a pipeline is miserable; that is a valid engineering reason. In interviews, write groupingBy from memory and say when you would refuse a stream.
                                """)
                        .qa("When is a stream worse than a loop?",
                                "Checked exceptions, mutation, early exit, and debugging. Also any time a reviewer cannot name the output type in one breath.",
                                "Why is parallel() a smell on a web request?")
                        .qa("How do you count applications by status?",
                                "groupingBy(Application::status, counting()). One collector, one pass. Nested HashMaps will drift.",
                                "Which collector builds a Map of lists?")
                        .sample("Count by status",
                                """
                                        Map<String, Long> byStatus = apps.stream()
                                                .collect(Collectors.groupingBy(App::status, Collectors.counting()));
                                        """,
                                "One pass. No mutable map in the loop."),
                Concepts.of("Optional at boundaries",
                                "Optional is a return type for 'this might be absent'. It is not a field, not a parameter, and not a substitute for a clear domain error. orElse versus orElseGet is the first trap.",
                                "A UserFinder returned null from three layers. A NPE in a Thymeleaf page was the report. Returning Optional from the repository and mapping to 404 in the controller made absence a contract.")
                        .depth("""
                                Optional exists so callers cannot ignore absence as easily as a null. Use it on service and repository return types when empty is a normal outcome: findByEmail, parseIfPresent. Do not use it for fields — it is not serializable in a useful way and it allocates. Do not use it as a parameter; overload or split the method. Do not wrap collections: return an empty list.

                                orElse(compute()) always computes the default. orElseGet(() -> compute()) is lazy. That is not style, it is a production bug when compute hits the database. orElseThrow is the right shape when absence is exceptional at this layer. map and flatMap chain transformations without nested ifPresent pyramids. get() is an assertion; treat it like a cast.

                                Optional is not for error messages. A failed payment is not an empty Optional — it is a domain result or an exception. Mixing 'not found' and 'forbidden' into Optional.empty() forces every caller to guess. In interviews, say boundary return type, never fields, and explain orElseGet.
                                """)
                        .qa("Why not store Optional in a JPA entity?",
                                "It is a return-type idiom, not a column. Persistence, JSON, and equals get awkward. Use a nullable field at the edge of storage, Optional at the service return.",
                                "When is Optional.empty the wrong model?")
                        .qa("orElse versus orElseGet?",
                                "orElse always evaluates the fallback. orElseGet defers it. If the fallback is a query or a throw, orElseGet is the one you meant.",
                                "When do you use orElseThrow?")
                        .sample("Absence as a contract",
                                """
                                        public Optional<Profile> findByEmail(String email) {
                                            return repository.findByEmail(email);
                                        }

                                        Profile require(String email) {
                                            return findByEmail(email).orElseThrow(() -> new NotFound(email));
                                        }
                                        """,
                                "The controller maps NotFound to 404. Empty is not a 500."),
                Concepts.of("Sealed types and pattern matching",
                                "Sealed interfaces name the closed set of subtypes. Pattern-matching switch keeps a protocol in one place. instanceof pyramids rot because the fourth type is added in one branch and forgotten in another.",
                                "Mail classification that branched recruiter versus interview versus noise stayed readable when a fourth intent showed up — the compiler demanded the new case on a sealed type.")
                        .depth("""
                                A sealed type lists its permitted subtypes. That is a protocol, not a taxonomy hobby. When the set is closed — payment events, mail intents, parser results — sealing turns a missed branch into a compile error. When the set is open because JSON from a vendor can grow, do not seal, and keep a default.

                                Pattern-matching switch binds the type and can deconstruct a record: case Invoice(Money total, String party). Guard clauses (when) keep matches readable. Null is a separate case in modern switches; forgetting it is an NPE, not a default. Exhaustiveness is the feature you are buying. instanceof chains fail open.

                                Do not nest deconstruction three levels deep. Cute matching is how reviews stall. Prefer a sealed interface plus one-level switch at the boundary, then ordinary methods on the subtype. In interviews, contrast this with visitor, and say when default is still required.
                                """)
                        .qa("How is this better than instanceof?",
                                "The branches live together. On a sealed interface, a missing case is a compile error. instanceof chains fail open.",
                                "When do you still need default?")
                        .qa("Can you switch on a record and unpack it?",
                                "Yes — case Invoice(Money total, String party). Keep it shallow. Nested deconstruction gets cute.",
                                "What happens if the invoice is null?")
                        .sample("Mail intents",
                                """
                                        sealed interface MailIntent permits Recruiter, Interview, Noise {}

                                        String route(MailIntent intent) {
                                            return switch (intent) {
                                                case Recruiter r -> tracker.open(r.company());
                                                case Interview i -> calendar.hold(i.at());
                                                case Noise ignored -> "ignore";
                                            };
                                        }
                                        """,
                                "A new permitted type is a compile reminder.")
        );
    }
}
