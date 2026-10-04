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
    private static final Book GENERIC = new Book(
            List.of(
                    std("Constructor injection",
                            "Pass collaborators in the constructor. No field injection, no setters for wiring.",
                            "Tests can swap fakes without opening the class."),
                    std("Timeouts on every remote call",
                            "Every HTTP, JDBC, and broker call has a timeout and a limit on in-flight work.",
                            "A hung dependency must not freeze the whole JVM."),
                    std("No secrets in source",
                            "Keys and passwords come from the environment. The jar is the same in every environment.",
                            "A leaked repo should be useless without the env."),
                    std("Named failures",
                            "Return a problem with a field path. Do not dump a stack trace on the user.",
                            "The UI can highlight the empty field instead of a 500.")
            ),
            List.of(
                    pat("Strategy", "Swap algorithms without rewriting callers.",
                            "A scorer interface with two implementations."),
                    pat("Adapter", "Hide a vendor SDK behind your type.",
                            "Fake the vendor in tests.")
            ));

    static {
        put("java",
                std("Constructor injection", "Pass collaborators in the constructor. No field injection, no setters for wiring.",
                        "Tests can swap fakes without opening the class."),
                std("Override equals and hashCode together", "If you override one, override both. Only include fields that define equality.",
                        "A HashMap key that mutates or hashes a lazy field is a lost entry."),
                std("Prefer immutability", "Records and final fields for values. Mutate only inside an aggregate that owns the lifecycle.",
                        "A DTO that grows setters becomes an accidental shared state."),
                std("Records at the edge", "Use records for DTOs and events. Mutable classes stay inside the domain when they must change.",
                        "Missing JSON fields fail at construct time, not in the ledger."),
                std("Time is Instant", "Store Instant or OffsetDateTime. Inject a Clock. Never new Date().",
                        "Interview slots in India stay honest across DST and servers."),
                std("Virtual threads for blocking I/O", "Prefer virtual threads over a homemade reactive rewrite when the work is blocking.",
                        "A servlet calling JDBC and HTTP stays readable."),
                std("Optional is a return type", "Never a field, never a parameter, never a substitute for a domain error.",
                        "orElse(query()) hits the database even on a hit. Use orElseGet."),
                std("Fail fast in constructors", "Invalid money must not enter the service. Compact constructors and requireNonNull.",
                        "A null currency at the edge is cheaper than a ledger incident."),
                std("Log once, wrap once", "Translate at the boundary. Do not catch-log-rethrow in every layer.",
                        "Duplicate stacks hide the first failure."),
                std("InterruptedException is a signal", "Restore the interrupt flag if you cannot stop. Do not swallow it.",
                        "A thread that loses its interrupt will not shut down."),
                std("Name the business", "PaymentId, not DataHelperManagerUtil. Methods are verbs with a result.",
                        "Interviewers read names before they read algorithms."),
                std("Prefer enums to magic ints", "A status is an enum with a closed set. int constants leak invalid values.",
                        "ApplicationStatus.SENT cannot be 17."),
                std("Close every AutoCloseable", "try-with-resources on JDBC, streams, and HTTP bodies. A forgotten close is a pool outage.",
                        "A portal client that returned empty listings had starved the connection pool."),
                std("Do not ignore exceptions", "Empty catch is a deleted alarm. Log once, wrap once, or name the recovery.",
                        "catch (Exception ignored) is how Search looked healthy while Adzuna was 403."),
                std("Defensive copies at the boundary", "Do not store a caller-owned list. Copy in, copy out, or use an unmodifiable view.",
                        "A controller that kept the request list saw it mutate after the handler returned."),
                std("Document thread-safety", "If a type is shared, say whether it is immutable, confined, or guarded.",
                        "A servlet-scoped HashMap is a bug; ConcurrentHashMap or confinement is the fix."),
                pat("SOLID — Single responsibility", "A class has one reason to change.",
                        "A portal client fetches JSON. A parser maps it. Mixing both is how Adzuna changes break mail."),
                pat("SOLID — Open / closed", "Add behavior by extension, not by editing a switch of types forever.",
                        "A new MatchScorer implementation, not another if (provider == GEMINI) in the gateway."),
                pat("SOLID — Liskov substitution", "A subtype must honor the parent contract, including exceptions and nulls.",
                        "A FakeClock that returns null Instant is not a Clock."),
                pat("SOLID — Interface segregation", "Clients must not depend on methods they do not use.",
                        "A read-only JobFinder is not a JobRepository that also deletes."),
                pat("SOLID — Dependency inversion", "Depend on abstractions you own. The container injects the adapter.",
                        "SearchService depends on JobPortalClient, not on AdzunaClient."),
                pat("Strategy", "Swap algorithms without rewriting callers.",
                        "A MatchScorer interface with heuristic and ChatClient implementations."),
                pat("Factory method", "A named constructor that hides which subtype you got.",
                        "SkillCurriculum.resolve(phrase) returns the outline without the caller switching on aliases."),
                pat("Builder", "Construct objects with many optional parts without telescoping constructors.",
                        "ChatClient prompts and Kafka ProducerRecords."),
                pat("Template method", "A skeleton with hooks for the varying step.",
                        "A portal client that shares HTTP error handling and lets Adzuna vs JSearch fill the query."),
                pat("Adapter", "Make a foreign API look like yours.",
                        "GoogleGenAiChatModel behind ChatClient. Fake it in tests."),
                pat("Decorator", "Add behavior without rewriting the core type.",
                        "SimpleLoggerAdvisor wrapping ChatClient calls."),
                pat("Observer", "Emit an event; do not call the next use case in a cycle.",
                        "ApplicationEvent after SENT, tracker listens."),
                pat("Composition over inheritance", "Hold a collaborator. Do not extend a concrete service to tweak one method.",
                        "DelegatingAiGateway holds Spring and heuristic gateways."),
                pat("Facade", "A simple front for a knot of types.",
                        "JobSearchService is the facade over several portal clients."),
                pat("Command", "An object that is a request you can store, retry, or switch on.",
                        "A sealed MailIntent dispatched in one pattern-matching switch."));
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
                        "Controllers do not talk to KafkaTemplate directly."),
                std("Fail fast on invalid config", "Validate @ConfigurationProperties at startup. A missing mail host should stop the JVM.",
                        "A Boot app that starts with a blank SMTP host fails at 4pm, not at boot."),
                std("No field injection", "@Autowired on fields hides the graph. Constructors make tests honest.",
                        "A missing bean becomes a NPE in production instead of a context failure."),
                std("One ObjectMapper", "Configure it once. Do not new ObjectMapper() in a controller.",
                        "Date formats drift per endpoint."),
                pat("Factory", "Boot's auto-config is a factory for beans you override when the default is wrong.",
                        "Read the conditions report before you write a duplicate @Bean."),
                pat("Circuit breaker", "Fail a remote call fast and shed load.",
                        "Resilience4J or a timeout plus bulkhead around JSearch."),
                pat("Health / probe", "Liveness vs readiness is the pattern Kubernetes already assumes.",
                        "A long Flyway migrate must not be ready."));
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
                        "@Tool methods for tracker counts."),
                std("Ground every answer", "Search the catalog (or a vector store) before you generate. That is AI search.",
                        "A tutor that quotes equals/hashCode from the lesson, not from a blog it invented."),
                std("Scope memory", "Chat memory is for the tutor. Scoring a job does not get yesterday's quiz.",
                        "A cover letter that says 'as we discussed in the mock' is a bug."),
                std("Read-only tools first", "Do not give the model a write until you trust the loop.",
                        "Tracker counts yes. Mark-applied no."),
                pat("RAG — retrieve then generate", "Select evidence, then ask the model to speak only from it.",
                        "LibrarySearch hits become the user message. ChatClient.entity fills the card."),
                pat("Structured output", "entity(Class) instead of regex on prose.",
                        "GeneratedConcept is a bean with points, trap, takeaway."),
                pat("Fallback gateway", "If the model 429s, the catalog still answers.",
                        "TutorService.catalogAnswer formats LibrarySearch hits."));
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
                        "@PreAuthorize is the last line of defense."),
                std("Authorize then authenticate order is a myth you should not debug by folklore",
                        "Read the filter chain. Document which matcher is first.",
                        "Two SecurityFilterChain beans without a securityMatcher is a 403 lottery."),
                std("Redirects after login stay relative", "Open redirects are a finding.",
                        "savedRequest must be a path you own."),
                pat("Authenticator / provider", "AuthenticationManager delegates to providers.",
                        "DaoAuthenticationProvider vs JwtAuthenticationProvider."),
                pat("Policy / voter", "Authorization is a decision, not a boolean in a controller.",
                        "AuthorizationManager on the chain, @PreAuthorize on the method."));
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
                pat("Proxy", "AOP and @Transactional.", "The proxy is why self-invocation skips the transaction."),
                std("Program to interfaces", "Inject the type you own. The implementation is a @Bean or a test fake.",
                        "This is DIP in Spring clothes."),
                std("No self-invocation on @Transactional", "this.save() skips the proxy. Inject self or move the method.",
                        "A nested save that does not commit is this bug."),
                pat("DIP / IoC", "The container inverts construction. You invert the dependency.",
                        "Same SOLID rule as Java, applied to @Bean graphs."),
                pat("Factory bean", "A @Bean method is a factory. Keep it small.",
                        "AiConfig.llmSupport is the factory for ChatClient."),
                pat("Interceptor / advisor", "Cross-cutting without inheritance.",
                        "HandlerInterceptor, MethodInterceptor, ChatClient advisors."));
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
                        "Resume PDFs do not ride in the event."),
                std("Idempotent consumer", "A retry must not double-send mail. Key the side effect.",
                        "enable.idempotence is the producer; you still need an event id on the consumer."),
                std("Commit after the side effect", "At-least-once is the default. Commit before SMTP and you can lose the mail.",
                        "Manual ack after success."),
                pat("Saga", "Long work as steps with compensations, not a distributed TX.",
                        "Apply → mail → tracker. A failed mail does not roll back the posting."),
                pat("Dead letter", "Poison messages leave the partition.",
                        "Retry topic, then DLT, then an alert."),
                pat("Event sourcing-lite", "The log is the truth for that stream. A table is a projection.",
                        "Do not treat Kafka as a database you update in place."),
                pat("Circuit breaker", "Stop calling a sick consumer of yours; shed, then retry with backoff.",
                        "A mail sender that 500s must not keep eating the apply topic."),
                std("Schema at the boundary", "A named payload type, not a JSON blob you hope is stable.",
                        "A renamed field without a version is a poison pill."),
                std("Lag is a product metric", "consumer_lag_max is on-call, not a curiosity.",
                        "A silent consumer is an outage you will discover from users."));
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
                        "Use it when you actually have two apps."),
                pat("Facade", "The app sees one JWT. Keycloak hides Google, GitHub, and the user federation.",
                        "Do not parse the IdP-specific payload in a controller."),
                std("Audience and issuer checks are not optional", "A token from the wrong realm is a stranger.",
                        "Spring resource-server decoder must pin issuer-uri."),
                std("Confidential clients keep the secret on the server", "A public SPA uses PKCE. A Boot app uses a secret.",
                        "A client secret in JavaScript is a public password."));
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
                        "A hung JSearch must not take the JVM."),
                pat("Operator / controller", "A control loop that makes the cluster match a CRD.",
                        "You write desired state; the operator creates the Deployments."),
                pat("Facade", "kube-apiserver is the facade. kubectl is a client, not the cluster.",
                        "Talk to the API, not to a node's Docker socket."),
                pat("Factory", "A controller creates Pods from a template. You do not new Pod in a bash loop.",
                        "Deployment is the factory; the replica set is the inventory."),
                std("Labels are the query index", "Selectors must match labels you actually set.",
                        "A Service with app=api that never lands on the pods is a silent 503."),
                std("One container, one job", "Sidecars are extra containers, not extra processes in yours.",
                        "A log shipper next to Boot, not a thread that tails /proc."));
        put("docker",
                std("Stable layers first", "JDK and Maven cache, jar last.", "A one-line Java change should be a tiny layer."),
                std("Non-root USER", "Numeric uid. Scanners expect it.", "A crafted image should not be root."),
                std("One process", "PID 1 is the app. Signals must reach Boot.", "SIGTERM finishes in-flight applies."),
                std("Pin by digest in prod", "latest is not a version.", "Prod must not float while you sleep."),
                pat("Builder", "Multi-stage build: compile then runtime image.", "No Maven in the final image."),
                pat("Immutable server", "Replace, do not patch.", "A new tag is a new container."),
                pat("Health check", "Compose vs Kubernetes probes are different tools.", "Do not assume they are the same."),
                pat("Facade", "The image is the unit you ship. Compose is the local facade over several images.",
                        "Do not SSH into a running container to patch classes."),
                std("No secrets in the image", "ARG for build, ENV from the orchestrator. A docker history must not print keys.",
                        "GEMINI_API_KEY is injected at run, never COPY .env."),
                std(".dockerignore is a security tool", "Keep .git, .env, and target/ out of the build context.",
                        "A leaked context is a leaked laptop."));
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
                        "Do not make the listing table do both forever."),
                pat("Repository", "The collection of an aggregate, not a God DAO.",
                        "JobPostingRepository does not send mail."),
                pat("Specification", "A query object you can compose, not a 12-parameter finder.",
                        "LocationFilter plus seniority as predicates."),
                std("Never SELECT * in a hot path", "Name the columns. Schema drift then fails at compile or map time.",
                        "A new blob column should not inflate the tracker list."),
                std("FK or document why not", "Orphan apply rows without a posting are a bug, not flexibility.",
                        "ON DELETE restrict until you have a real archive story."));
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
                pat("Claim check / lease", "A lock is a short lease.", "apply:{id}"),
                pat("Proxy", "A cache is a proxy in front of a slower store.",
                        "Miss goes to Postgres. Hit never does."),
                std("Single-thread Lua or WATCH for check-then-set", "GET then SET from two clients is a race.",
                        "A rate limiter that is not atomic is decoration."),
                std("Do not store sessions you cannot rebuild", "If Redis dies, login should still work from the IdP.",
                        "Sticky sessions in Redis without a fallback are an outage."));
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
                pat("Iterator / paginator", "List APIs are paged.", "listObjectsV2Paginator."),
                pat("Facade", "Your ResumeStorage interface is the facade. S3 is an implementation.",
                        "Tests never see AmazonS3Client."),
                pat("Factory", "SDK clients are built once from env. Not per request.",
                        "S3Client.builder() in a @Bean, not in a controller."),
                std("Retry only idempotent calls", "PutObject with a known key yes. CreateBucket in a loop no.",
                        "A timeout retry that creates a second bucket is a bill."));
        put("rest",
                std("Nouns in the path", "POST /applications, not /doApply.", "You can GET the resource later."),
                std("Idempotency-Key on POST", "Clients retry. You must not double-send mail.",
                        "A recruiter double-click is one apply."),
                std("Problem+json", "Status, type, field path.", "The UI highlights the empty jobId."),
                std("Page lists", "Never dump 10k jobs.", "ETag on GET so polling is cheap."),
                pat("Resource", "The REST noun.", "Application, Job, Resume."),
                pat("Gateway", "A BFF in front of several services.", "This app is the BFF for portals."),
                pat("HATEOAS-lite", "Link rel=next is enough. Full hypermedia is optional.",
                        "Tracker pagination."),
                std("Version in Accept or URL", "Breaking JSON is a new version. Additive fields are not.",
                        "Clients on v1 must not 500 because you renamed amount."),
                std("Auth on every mutating route", "GET can be cacheable. POST needs the principal.",
                        "A guessed /apply/run must 401, not send mail."),
                pat("Repository", "The collection of a resource, not a god DAO.",
                        "ApplicationRepository is not SearchService."),
                pat("DTO / assembler", "The wire type is not the entity.",
                        "Records at the edge, entities inside."),
                pat("Idempotent receiver", "POST with Idempotency-Key is a pattern, not a header you ignore.",
                        "The same key returns the same application id."));
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
                pat("Bulkhead", "Isolate thread pools and connections.", "Portal HTTP on its own pool."),
                pat("Anti-corruption layer", "Translate the other team's model at the edge. Do not leak their JSON inward.",
                        "PortalJob is yours. Adzuna JSON stays in the client."),
                pat("Strangler fig", "Route one use case to a new service, not a rewrite weekend.",
                        "Mail leaves the monolith first; Search stays."),
                std("Version public events", "Additive fields yes. Rename no without a new type.",
                        "A consumer on v1 must not 500 because you renamed amount."),
                std("Own your timeouts", "The other team's SLA is not your timeout. Set yours shorter.",
                        "A 30s HTTP client on a 5s SLO is how you miss the SLO."));
    }

    private SkillPlaybook() {
    }

    public static List<StandardRule> standards(String skillKey) {
        return book(skillKey).standards();
    }

    public static List<PatternRule> patterns(String skillKey) {
        return book(skillKey).patterns();
    }

    public static boolean authored(String skillKey) {
        return skillKey != null && BY_KEY.containsKey(skillKey);
    }

    private static Book book(String skillKey) {
        if (skillKey != null) {
            Book match = BY_KEY.get(skillKey);
            if (match != null) {
                return match;
            }
        }
        return GENERIC;
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
