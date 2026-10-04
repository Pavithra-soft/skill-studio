package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class MicroservicesCatalog {

    private MicroservicesCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("microservices", "Microservices",
                "A microservice is an independently deployable boundary, not a folder. If you cannot deploy it alone, it is a module.",
                Concepts.of("Bounded contexts",
                                "Split on business change, not on nouns. Search and Apply can be two services when their deploy cadence or model diverges.",
                                "A job-monitor-style app stayed a modular monolith on purpose until Mail's poll would have hurt Search deploys. The split line was already a package boundary.")
                        .depth("""
                                A bounded context is where a word changes meaning. Job in Search is a posting; Job in Payroll is a cost object. Sharing a Job table across 'services' is a distributed monolith: you coupled deploys and data.

                                Extract when two teams cannot release independently, or the model fights itself. 'We might scale apply' is not enough. Measure. Take the database with you. Until then, packages with rules beat a network hop.

                                In interviews, defend a modular monolith without sounding afraid of services, and name the first split you would actually do.
                                """)
                        .qa("When do you actually extract a service?",
                                "When deploy cadence or the data model diverges, and you can take the data with you. Not when the folder list looks pretty.",
                                "What do you do with the shared Job table?")
                        .qa("How do modules talk inside a monolith?",
                                "Public application services and events at the boundary. Do not import another module's persistence types.",
                                "How is that different from HTTP between services?")
                        .sample("Event at the module edge",
                                """
                                        publisher.publishEvent(new ApplicationSubmitted(id));
                                        """,
                                "Mail listens. Search does not import Mail entities."),
                Concepts.of("Outbox over dual writes",
                                "Do not write DB then Kafka. Write DB plus outbox in one transaction, then publish. Dual write will eventually lose one side.",
                                "An apply that committed but never emitted the event was worse than a failure the user could retry. Outbox made emit follow commit.")
                        .depth("""
                                The outbox table is in the same database transaction as the business row. A publisher polls (or CDC reads the WAL) and writes to Kafka. Delete or mark published after the broker ack. Keep payloads small or use a claim check.

                                Dual write (save then send) fails in both directions: send succeeds and TX rolls back, or TX commits and send fails. You cannot fix that with a try/catch.

                                In interviews, this is the expected answer for 'how do you publish after save'.
                                """)
                        .qa("Why not send Kafka in the same service method after save?",
                                "save can commit and send can fail, or send can succeed and the transaction can roll back. Outbox is one commit.",
                                "CDC versus an outbox table?")
                        .qa("What if the publisher crashes?",
                                "Rows stay unpublished. Another publisher instance resumes. That is at-least-once — consumers must be idempotent.",
                                "How do you avoid poison outbox rows?")
                        .sample("Same transaction",
                                """
                                        @Transactional
                                        public void apply(ApplyCommand cmd) {
                                            var row = applications.save(cmd);
                                            outbox.append("ApplicationSubmitted", row.id(), row.payload());
                                        }
                                        """,
                                "Publisher sends after commit."),
                Concepts.of("Timeouts and bulkheads",
                                "Every remote call has a timeout and a limit on outstanding work. Otherwise one slow portal takes the JVM with it.",
                                "JSearch hanging froze the Apply page until portal HTTP sat on its own pool with a hard timeout.")
                        .depth("""
                                A timeout is a decision: how long the user (or the worker) waits. A bulkhead is a decision: how many concurrent calls to that dependency. Thread pools, semaphores, and connection pools are bulkheads. Virtual threads do not remove the need — the downstream still has a capacity.

                                Retries without jitter and without a cap are an outage amplifier. Circuit breakers stop calling a sick dependency; they are not a timeout substitute.

                                In interviews, name timeout, pool size, and retry cap for one HTTP client.
                                """)
                        .qa("Why did one slow HTTP call freeze the site?",
                                "The request thread (or the only pool) waited without a timeout, or every thread sat in that call. Isolate the pool and cap wait.",
                                "Do virtual threads fix this?")
                        .qa("Retry budget?",
                                "Max attempts, backoff, jitter, and a total time smaller than the caller's timeout. Otherwise you pile on.",
                                "When is a circuit breaker useful?")
                        .sample("RestClient timeout",
                                """
                                        RestClient.builder()
                                                .requestFactory(factoryWithTimeouts(Duration.ofSeconds(2)))
                                                .build();
                                        """,
                                "Two seconds, then fail. Screen only — wire the factory you actually use."),
                Concepts.of("Correlation ids",
                                "One id on the log line from the browser to Kafka. Without it you cannot debug a single apply.",
                                "A user saying 'it failed at 4pm' became a grep after X-Correlation-Id was copied into MDC and Kafka headers.")
                        .depth("""
                                Generate at the edge if the client did not send one. Propagate on HTTP headers, Kafka headers, and MDC. Do not invent a second id per hop. Truncate in logs if you must, but keep enough entropy.

                                Trace systems (OpenTelemetry) are the grown-up form. A raw correlation id is the minimum that works in a monolith plus one broker.

                                In interviews, list the three places you put it.
                                """)
                        .qa("Who generates the id?",
                                "The client, or the first edge service if missing. Downstream trusts it after validating length and charset.",
                                "Where does it go on Kafka?")
                        .qa("Is a correlation id a security token?",
                                "No. It is a tracing handle. Do not treat it as authentication. Still avoid putting PII in it.",
                                "MDC leak risk?")
                        .sample("Header in, MDC on",
                                """
                                        String corr = Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                                                .filter(id -> id.length() <= 80)
                                                .orElseGet(() -> UUID.randomUUID().toString());
                                        MDC.put("corr", corr);
                                        """,
                                "Clear MDC in finally."),
                Concepts.of("Contract tests",
                                "Producer and consumer agree on a schema. Pact or Spring Cloud Contract, or a schema registry — not a wiki.",
                                "Mail could deploy on Tuesday because Search still understood Monday's event. The contract test ran in CI on both repos.")
                        .depth("""
                                Contract tests freeze the bits you cannot break: field names, types, requiredness. Consumer-driven contracts (Pact) start from the consumer's expectations. Producer-side schema tests start from the event. Pick one and run it in CI of both parties.

                                A wiki table is not a contract. It will drift. JSON snapshots without compatibility rules will fail late.

                                In interviews, name the tool and which pipeline breaks on a rename.
                                """)
                        .qa("Who owns the contract?",
                                "Both. The consumer states need; the producer proves they still meet it. CI on both sides, not a Slack agreement.",
                                "Wiki versus Pact?")
                        .qa("Can OpenAPI replace this?",
                                "OpenAPI is a contract for HTTP if you actually test against it (generated clients, schema validation). A file nobody runs is a wiki.",
                                "What about Kafka?")
                        .sample("Stable event name",
                                """
                                        public record ApplicationSubmitted(String applicationId, String jobId, Instant at) {}
                                        """,
                                "Rename is a new event type, not a silent field change."),
                Concepts.of("Data ownership",
                                "Each service owns its tables. If you query another service's database, you just built a distributed monolith with extra latency.",
                                "Apply needed job titles. It stored a snapshot on the event instead of joining Search's database from a second connection pool.")
                        .depth("""
                                Ownership means writes and schema. Reads of someone else's data go through their API or a replica they publish (events, a read model). Sharing a database user 'temporarily' becomes architecture.

                                Snapshots on events (job title at apply time) are often correct: history should not change when Search renames a listing. Live lookup is for 'what is true now'.

                                In interviews, refuse the shared DB join and offer an event snapshot or an API.
                                """)
                        .qa("Can two services share a Postgres schema?",
                                "They can, and you have one deployable wearing two hats. Independent deploys need independent data.",
                                "When is a snapshot on the event better than a live GET?")
                        .qa("What is a read model?",
                                "A table you own, filled from someone else's events, shaped for your queries. You can drop it and rebuild if the events are the source.",
                                "Who runs migrations?")
                        .sample("Snapshot on the command",
                                """
                                        public record ApplyCommand(String jobId, String jobTitle, String company) {}
                                        """,
                                "Title at apply time, not a join later."),
                Concepts.of("API gateway and BFF",
                                "A gateway is TLS, routing, authn, rate limits. A BFF is a backend shaped for one UI. Neither is a place to hide a god service.",
                                "This classroom is a BFF for learning: one UI, several skill catalogs. It does not pretend to be a company-wide gateway.")
                        .depth("""
                                Gateways terminate TLS, check JWTs, route prefixes, apply rate limits. They should not contain apply-business-logic. BFFs aggregate and reshape for a frontend so the browser does not chat with twelve origins.

                                Overgrowing a BFF into the only app is a monolith with extra hops. That can be fine. Call it that.

                                In interviews, split gateway duties from domain services.
                                """)
                        .qa("What belongs in a gateway?",
                                "Authn, TLS, routing, coarse rate limits, correlation id. Not job-matching rules.",
                                "When do you want a BFF?")
                        .qa("Is a BFF a microservice?",
                                "It is independently deployable if you treat it that way. It is still allowed to be a modular monolith behind the UI.",
                                "Why not let the SPA call every service?")
                        .sample("Classroom BFF path",
                                """
                                        GET /api/lesson.json
                                        GET /api/project.zip
                                        """,
                                "One origin for the UI. Catalogs stay in-process here."),
                Concepts.of("Circuit breaker",
                                "After N failures, stop calling for a cooldown. Combined with timeouts, it protects you. Alone, it just fails faster without a plan.",
                                "A hung portal tripped the breaker; search returned heuristic results instead of waiting thirty seconds per listing.")
                        .depth("""
                                Closed: calls pass. Open: fail fast. Half-open: a probe. Resilience4j is the usual Java library. The breaker is per dependency, not global. A global breaker turns one sick host into a site outage.

                                Fallback must be explicit: cached data, heuristic, 503. Silent empty lists are how you hide outages.

                                In interviews, states of the breaker, and the fallback you would actually show.
                                """)
                        .qa("What does open mean?",
                                "We are not calling the dependency. Calls fail fast or hit fallback until the cooldown and a probe succeed.",
                                "Why per-dependency?")
                        .qa("Is a breaker a timeout?",
                                "No. Timeouts bound one call. Breakers bound a streak of failures. You want both.",
                                "What is a bad fallback?")
                        .sample("Named breaker",
                                """
                                        // Resilience4j: CircuitBreakerRegistry.ofDefaults()
                                        // annotate or decorate the portal client, not the whole JVM
                                        """,
                                "Keep the fallback visible in the UI.")
        );
    }
}
