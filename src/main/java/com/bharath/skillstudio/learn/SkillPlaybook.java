package com.bharath.skillstudio.learn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coding standards and design patterns used in production for each catalog skill.
 */
public final class SkillPlaybook {

    public record Book(List<StandardRule> standards, List<PatternRule> patterns) {
    }

    private static final Map<String, Book> BY_KEY = new LinkedHashMap<>();

    static {
        put("java",
                std("Constructor injection", "Pass collaborators in the constructor. No field injection, no setters for wiring.",
                        "Tests can swap fakes without opening the class."),
                std("Records at the edge", "Use records for DTOs and events. Mutable classes stay inside the domain when they must change.",
                        "Missing JSON fields fail at compile or bind time, not in prod."),
                std("Time is Instant", "Store Instant or OffsetDateTime. Inject a Clock. Never new Date().",
                        "Interview slots in India stay honest across DST and servers."),
                std("Virtual threads for blocking I/O", "Prefer virtual threads over a homemade reactive rewrite when the work is blocking.",
                        "A servlet calling JDBC and HTTP stays readable."),
                pat("Strategy", "Swap algorithms without rewriting callers.",
                        "A MatchScorer interface with heuristic and ChatClient implementations."),
                pat("Builder", "Construct objects with many optional parts without telescoping constructors.",
                        "ChatClient prompts and Kafka ProducerRecords."),
                pat("Template method", "A skeleton with hooks for the varying step.",
                        "A portal client that shares HTTP error handling and lets Adzuna vs JSearch fill the query."));
        put("spring-boot",
                std("Typed properties", "One @ConfigurationProperties bean per area. Validate it. No scattered @Value.",
                        "Mail, search, and AI settings cannot drift."),
                std("Actuator is a contract", "Expose liveness and readiness separately. Health must name the dependency that is down.",
                        "Kubernetes can stop traffic without killing a healthy JVM."),
                std("Test the slice", "@WebMvcTest and @DataJpaTest by default. Full context is the last mile.",
                        "People run tests when they are seconds, not minutes."),
                std("Config outside the image", "Secrets and URLs from env. The jar is the same in every environment.",
                        "A GEMINI key rotation does not need a rebuild."),
                pat("Externalized configuration", "The 12-factor rule Boot already implements.",
                        "application.yml plus env plus optional .env import."),
                pat("Convention over configuration", "Starters turn on beans. You override when the default is wrong.",
                        "Know the conditions report before you write a @Bean."),
                pat("Layered architecture", "web → application → domain → adapters.",
                        "Controllers do not talk to KafkaTemplate directly."));
        put("spring-ai",
                std("One ChatClient", "Build it once. Different prompts, not different clients.",
                        "Scoring and cover letters stay consistent."),
                std("Structured output first", "Prefer entity() over parsing prose.",
                        "A score is a column, not a paragraph."),
                std("Never log secrets", "Prompts may contain resumes. Tighten loggers before deploy.",
                        "SimpleLoggerAdvisor is a flight recorder for a laptop, not a company cluster."),
                std("Retrieve before you generate", "If you did not select evidence, you did not do RAG.",
                        "A cover letter that mentions Kafka because the posting asked, not because the resume dumped acronyms."),
                pat("Advisor / interceptor", "Cross-cutting on the ChatClient.",
                        "Logger, memory, and tools are advisors, not a second client."),
                pat("Gateway", "Hide the model behind an interface.",
                        "AiGateway with Spring and heuristic implementations."),
                pat("Tool / command", "Let the model call a named action instead of inventing facts.",
                        "@Tool methods for tracker counts."));
        put("spring-security",
                std("One chain per app", "authorizeHttpRequests plus one authentication style. Document exceptions.",
                        "Two ad-hoc filters are how 403s become undebuggable."),
                std("Least privilege", "Deny by default. Open paths by name.",
                        "A guessed /apply/run must not fire."),
                std("Never log tokens", "Access, refresh, and session ids stay out of logs.",
                        "A leaked aggregator dump should be useless."),
                std("CSRF matches the client", "Browser forms on. Bearer APIs off, on purpose.",
                        "Mixing them is a mysterious 403."),
                pat("Filter chain", "The security interceptor pipeline.",
                        "SecurityFilterChain is the product."),
                pat("Adapter", "OAuth2 resource server adapts JWT to Authentication.",
                        "You do not write a parser."),
                pat("Decorator", "Method security wraps the service.",
                        "@PreAuthorize is the last line of defense."));
        put("spring-framework",
                std("Constructor injection", "The container owns lifetimes.", "Field injection makes tests sticky."),
                std("Transactions on the service", "Not on the controller. Know REQUIRED vs REQUIRES_NEW.",
                        "A scored job and its parent posting commit together."),
                std("Events after commit", "Transactional events, not dual writes.",
                        "No mail send for a rolled-back apply."),
                std("Advice for HTTP errors", "@ControllerAdvice, not a stack trace on Search.",
                        "Empty jobId is a 400 with a field name."),
                pat("Dependency injection", "Inversion of control.", "Swap Kafka for a fake in tests."),
                pat("Observer", "Application events.", "SENT status triggers tracker without a cyclic call."),
                pat("Proxy", "AOP and @Transactional.", "The proxy is why self-invocation skips the transaction."));
        put("kafka",
                std("Key for order", "The same entity id is the key. Order is per partition, not per topic.",
                        "A cancel cannot beat a booking."),
                std("Idempotent producer on", "Never disable it to 'fix' a timeout.",
                        "Retries must not create two apply rows."),
                std("Poison pills leave the partition", "Retry topic then DLT. Do not block the log.",
                        "One bad JSearch JSON cannot freeze mail."),
                std("One purpose per group.id", "A second independent read is a second group.",
                        "Search and fraud do not steal partitions."),
                pat("Publish-subscribe", "Many consumers, one log.", "Two groups on payments."),
                pat("Outbox", "DB commit then publish from the same transaction.",
                        "Pair with microservices when you also write Postgres."),
                pat("Claim check", "Store the fat payload elsewhere, pass an id on the bus.",
                        "Resume PDFs do not ride in the event."));
        put("keycloak",
                std("Realm is a tenant", "Do not share realms across products.", "Tokens must not work on the wrong app."),
                std("PKCE for public clients", "No password grant. No refresh in localStorage.",
                        "A SPA on the internet is not a confidential client."),
                std("Short access TTL", "Minutes, not hours. Rotate secrets.", "A leaked JWT should already be dead."),
                std("Map roles once", "Token mapper to ROLE_ names Spring expects.",
                        "Do not invent a second user table."),
                pat("Identity provider / broker", "One JWT shape in front of Google or GitHub.",
                        "The app stays dumb about social login."),
                pat("Adapter", "Keycloak as the OIDC adapter for Spring Security.",
                        "Resource server validates issuer and audience."),
                pat("Single sign-on", "One session across clients in the realm.",
                        "Use it when you actually have two apps."));
        put("kubernetes",
                std("Pods are disposable", "State lives in a volume or a database.", "H2 files and Postgres outlive the pod."),
                std("Split the probes", "Liveness kills. Readiness removes from the Service.",
                        "A slow Boot start must not 502 Search."),
                std("Requests are the truth", "Set them. Limits are the ceiling.",
                        "CPU throttle looks like a mystery GC pause."),
                std("Secrets are not ConfigMaps", "Mount them. Do not bake them.",
                        "Key rotation without a new image."),
                pat("Reconciliation loop", "Desired state vs actual.", "A Deployment puts pods back."),
                pat("Sidecar", "Helper process in another container, not PID 2 in yours.",
                        "A log shipper next to Boot."),
                pat("Circuit breaker at the edge", "Mesh or app-level timeouts.",
                        "A hung JSearch must not take the JVM."));
        put("docker",
                std("Stable layers first", "JDK and Maven cache, jar last.", "A one-line Java change should be a tiny layer."),
                std("Non-root USER", "Numeric uid. Scanners expect it.", "A crafted image should not be root."),
                std("One process", "PID 1 is the app. Signals must reach Boot.", "SIGTERM finishes in-flight applies."),
                std("Pin by digest in prod", "latest is not a version.", "Prod must not float while you sleep."),
                pat("Builder", "Multi-stage build: compile then runtime image.", "No Maven in the final image."),
                pat("Immutable server", "Replace, do not patch.", "A new tag is a new container."),
                pat("Health check", "Compose vs Kubernetes probes are different tools.", "Do not assume they are the same."));
        put("postgresql",
                std("Index what you filter", "If EXPLAIN ignores it, it is a comment.",
                        "Company plus posted_at should not seq-scan."),
                std("Expand-contract migrations", "Never edit a shipped Flyway version.",
                        "V2 adds a table. V1 stays."),
                std("JSONB is not a column you filter every time", "Promote hot fields.",
                        "Match score is a real column."),
                std("VACUUM is not optional", "Long transactions freeze cleanup.",
                        "A hot tracker table dies without it."),
                pat("Unit of work", "A transaction is the pattern.", "SELECT FOR UPDATE for the apply queue."),
                pat("Outbox", "Write the event row in the same commit.", "Kafka publish happens after."),
                pat("CQRS-lite", "A read model for search, write model for apply.",
                        "Do not make the listing table do both forever."));
        put("redis",
                std("TTL on every cache key", "A cache without TTL is a forgotten database.",
                        "Adzuna results expire in minutes."),
                std("Keys include the value shape version", "job:v2:123 not job:123.",
                        "A deploy does not read yesterday's JSON."),
                std("Locks expire", "SET NX EX. Never hold across a click.",
                        "Two tabs must not enqueue twice."),
                std("If you cannot rebuild it, do not put it in Redis", "Tracker state belongs in Postgres.",
                        "Redis can die."),
                pat("Cache-aside", "Miss, load, set.", "Skills-gap results."),
                pat("Write-through", "When stale reads hurt more than extra writes.", "Live apply status."),
                pat("Claim check / lease", "A lock is a short lease.", "apply:{id}"));
        put("aws-sdk",
                std("Default credentials chain", "Env, profile, then role. Never a key in code.",
                        "Laptop and ECS share a binary."),
                std("One client bean", "Thread-safe. Not per request.", "Resume upload uses one S3 client."),
                std("Timeouts larger than the retry budget", "Or you pile on.", "A flaky put retries once."),
                std("IAM least privilege", "PutObject on one prefix, not *.",
                        "A leaked role should not read the account."),
                pat("Adapter", "SDK client behind an interface.", "Fake S3 in tests."),
                pat("Retry / backoff", "The SDK already does it. Configure, do not wrap blindly.",
                        "Know the budget."),
                pat("Iterator / paginator", "List APIs are paged.", "listObjectsV2Paginator."));
        put("rest",
                std("Nouns in the path", "POST /applications, not /doApply.", "You can GET the resource later."),
                std("Idempotency-Key on POST", "Clients retry. You must not double-send mail.",
                        "A recruiter double-click is one apply."),
                std("Problem+json", "Status, type, field path.", "The UI highlights the empty jobId."),
                std("Page lists", "Never dump 10k jobs.", "ETag on GET so polling is cheap."),
                pat("Resource", "The REST noun.", "Application, Job, Resume."),
                pat("Gateway", "A BFF in front of several services.", "This app is the BFF for portals."),
                pat("HATEOAS-lite", "Link rel=next is enough. Full hypermedia is optional.",
                        "Tracker pagination."));
        put("microservices",
                std("Split on deploy cadence", "If you cannot ship it alone, it is a module.",
                        "This app is a modular monolith on purpose."),
                std("Outbox, not dual write", "DB plus outbox in one transaction.",
                        "An apply that commits must emit."),
                std("Timeout every remote call", "Plus a limit on outstanding work.",
                        "JSearch hanging cannot freeze Apply."),
                std("Correlation id on every log", "Browser to Kafka.", "A 4pm failure becomes a grep."),
                pat("Bounded context", "A business boundary.", "Search vs Apply."),
                pat("Saga / choreography", "Long-running business with events, not a distributed TX.",
                        "Apply → mail → tracker."),
                pat("Bulkhead", "Isolate thread pools and connections.", "Portal HTTP on its own pool."));
    }

    private SkillPlaybook() {
    }

    public static List<StandardRule> standards(String skillKey) {
        Book book = BY_KEY.get(skillKey);
        return book == null ? List.of() : book.standards();
    }

    public static List<PatternRule> patterns(String skillKey) {
        Book book = BY_KEY.get(skillKey);
        return book == null ? List.of() : book.patterns();
    }

    private static void put(String key, Object... items) {
        List<StandardRule> standards = new ArrayList<>();
        List<PatternRule> patterns = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof StandardRule rule) {
                standards.add(rule);
            } else if (item instanceof PatternRule rule) {
                patterns.add(rule);
            }
        }
        BY_KEY.put(key, new Book(List.copyOf(standards), List.copyOf(patterns)));
    }

    private static StandardRule std(String title, String rule, String why) {
        return new StandardRule(title, rule, why);
    }

    private static PatternRule pat(String name, String intent, String how) {
        return new PatternRule(name, intent, how);
    }
}
