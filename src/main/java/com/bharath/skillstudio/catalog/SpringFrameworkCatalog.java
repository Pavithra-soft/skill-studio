package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class SpringFrameworkCatalog {

    private SpringFrameworkCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("spring-framework", "Spring Framework",
                "Framework is the engine Boot wraps: a container, a transaction, a servlet dispatcher, and the proxies people forget exist.",
                Concepts.of("Dependency injection",
                                "The container owns lifetimes. You inject constructors. Field injection is how tests get sticky.",
                                "Swapping a real Kafka client for a fake in a test took one constructor argument. The class that used field @Autowired needed the whole context.")
                        .depth("""
                                A bean is an object the container constructs, injects, and disposes. Constructor injection makes dependencies required and obvious. A unit test passes fakes without opening the class. Field injection hides the graph and needs reflection or a full context. Setter injection is for optional collaborators — rare if you split classes well.

                                Prefer one constructor. If you need two, you probably need two types. @Autowired on a single constructor is optional in modern Spring. Circular dependencies are a design smell; constructor injection makes them fail loudly, which is a gift. @Lazy is a workaround, not a solution.

                                new inside a @Service is not DI. The container cannot swap that collaborator. Factories and ObjectProvider exist for beans that must be created per call. In interviews, say constructor, no field injection, and how a test looks.
                                """)
                        .qa("Why not field @Autowired?",
                                "The test has to open the class or start the container. A constructor argument is a fake in one line. Required dependencies cannot be forgotten.",
                                "What is the difference between a bean and a new?")
                        .qa("How do you swap Kafka in a test?",
                                "Pass a fake through the constructor, or a @TestConfiguration @Bean of the same type. Do not mock the class under test to avoid the constructor.",
                                "When is @Autowired on the constructor still OK?")
                        .sample("Obvious collaborators",
                                """
                                        public SearchService(JobRepository jobs, Clock clock) {
                                            this.jobs = jobs;
                                            this.clock = clock;
                                        }
                                        """,
                                "A unit test passes a list and a fixed clock."),
                Concepts.of("Transactions",
                                "@Transactional on the service that must be atomic, not on the controller. Know REQUIRED versus REQUIRES_NEW, and know the proxy.",
                                "Saving a job posting and its score in one unit of work stopped a crash from leaving a scored row without a parent.")
                        .depth("""
                                A transaction is a unit of work against a resource, usually JDBC. Spring's @Transactional opens it before the method and commits or rolls back after. REQUIRED joins an existing transaction or starts one. REQUIRES_NEW suspends and starts a nested independent one — easy to overuse, deadly if the inner commits and the outer rolls back.

                                The annotation works through a proxy. Self-invocation (this.save()) skips the proxy, so the inner method is not transactional. Place the annotation on the public method of a separate bean that callers go through. Checked exceptions do not roll back by default; RuntimeException does. rollbackFor is how you change that.

                                Controllers should not be transactional. The transaction would include view rendering and HTTP. Keep it on the service that talks to the repository. In interviews, mention the proxy, self-invocation, and isolation briefly (READ_COMMITTED is the usual JDBC default).
                                """)
                        .qa("Why not @Transactional on the controller?",
                                "You hold a database transaction across HTTP and templates. Put it on the service that must be atomic.",
                                "What is self-invocation?")
                        .qa("REQUIRED versus REQUIRES_NEW?",
                                "REQUIRED joins or starts. REQUIRES_NEW always starts a new one. The inner commit survives an outer rollback — only use it when that is the product rule.",
                                "Which exceptions roll back by default?")
                        .sample("One unit of work",
                                """
                                        @Transactional
                                        public JobPosting scoreAndSave(JobPosting posting, int score) {
                                            posting.setScore(score);
                                            return jobs.save(posting);
                                        }
                                        """,
                                "Parent and score commit together."),
                Concepts.of("AOP and the proxy",
                                "Logging, metrics, and retry belong in aspects or interceptors, not copied in twelve services. The proxy is why self-invocation skips the advice.",
                                "A timing aspect on portal clients showed Adzuna versus JSearch latency without decorating every method. A self-call inside the same class never hit the aspect — they moved the method to a neighbor bean.")
                        .depth("""
                                Spring AOP is proxy-based by default. A JDK proxy if you have an interface, a CGLIB subclass otherwise. Calls that enter through the proxy run the advice. Calls from this do not. Transactional, async, and custom @Around share this rule.

                                Use AOP for true cross-cutting: timing, audit, retry. Do not put business rules in an aspect — they become invisible. Filter and HandlerInterceptor are the HTTP equivalents; pick the layer that sees the data you need.

                                Debugging AOP is reading the proxy. In interviews, draw the call: controller → proxy → service, and the self-call that skips it. Mention that @Transactional is AOP, not magic JDBC.
                                """)
                        .qa("Why did my @Transactional method not open a transaction?",
                                "You called this.method() inside the same class. The proxy never ran. Move the method, or inject self through the container (last resort).",
                                "JDK proxy versus CGLIB?")
                        .qa("When is an aspect the wrong tool?",
                                "When the rule is business logic one service owns. Aspects are for the same wrapping on many types.",
                                "Filter versus interceptor versus aspect?")
                        .sample("Timed portal calls",
                                """
                                        @Around("@annotation(Timed)")
                                        public Object time(ProceedingJoinPoint pjp) throws Throwable {
                                            long start = System.nanoTime();
                                            try {
                                                return pjp.proceed();
                                            } finally {
                                                log.info("{} took {} ms", pjp.getSignature().getName(),
                                                        (System.nanoTime() - start) / 1_000_000);
                                            }
                                        }
                                        """,
                                "Business methods stay undecorated."),
                Concepts.of("Application events",
                                "Events decouple 'something happened' from 'send mail'. Transactional events fire after commit, so a rollback does not emit a ghost.",
                                "After an application was marked SENT, a listener recorded the tracker row. When the transaction rolled back, no listener ran.")
                        .depth("""
                                ApplicationEventPublisher.publishEvent is in-process pub/sub. Listeners are beans. By default they run synchronously on the publishing thread, inside the same transaction if one is open — which can surprise you. @TransactionalEventListener(phase = AFTER_COMMIT) waits until the write is real.

                                Events are not a message bus. If the process dies after commit and before the listener, you lost the side effect. That is when you want an outbox to Kafka. Inside a modular monolith, events keep packages from importing each other's internals.

                                Do not build a giant event taxonomy. Name past-tense facts: ApplicationSubmitted. In interviews, contrast sync listeners, AFTER_COMMIT, and a real broker.
                                """)
                        .qa("Why AFTER_COMMIT?",
                                "So a rolled-back apply does not send mail. The listener sees committed state.",
                                "What if the process dies after commit and before the listener?")
                        .qa("When do you use Kafka instead of ApplicationEvent?",
                                "When another process or another deployable must see it, or when you cannot lose the side effect. In-process events are not durable.",
                                "Are listeners async by default?")
                        .sample("After commit",
                                """
                                        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
                                        public void onSubmitted(ApplicationSubmitted event) {
                                            tracker.markQueued(event.id());
                                        }
                                        """,
                                "No ghost tracker row."),
                Concepts.of("DispatcherServlet",
                                "MVC is still a servlet. Filters, interceptors, and exception handlers live on that path. WebFlux is a different runtime.",
                                "A 400 on an empty jobId became an @ControllerAdvice instead of a stack trace on the page.")
                        .depth("""
                                DispatcherServlet is the front controller. It maps a request to a handler, runs interceptors, invokes the method, and writes the response. Filters wrap the servlet (security, CORS). HandlerInterceptors see the handler. @ControllerAdvice sees exceptions and selected return types.

                                Keep controllers thin: HTTP in, a service call, HTTP out. Validation via @Valid belongs here. Business rules do not. View names and JSON are both valid return styles; this studio is JSON plus Thymeleaf for the classroom.

                                WebFlux's DispatcherHandler is not a servlet chain. Mixing WebFlux annotations on a servlet app does not make it reactive. In interviews, order the pipeline: filter, servlet, interceptor, controller, advice.
                                """)
                        .qa("Filter versus interceptor versus advice?",
                                "Filters wrap the servlet, including Security. Interceptors see the chosen handler. Advice sees exceptions and selected responses.",
                                "Where does Spring Security sit?")
                        .qa("When is WebFlux the wrong answer in a Boot MVC app?",
                                "When you have not changed the runtime. Returning a Flux from a servlet controller still ties up a thread unless you subscribe elsewhere.",
                                "What does @ControllerAdvice replace?")
                        .sample("Name the field",
                                """
                                        @ControllerAdvice
                                        class ApiAdvice {
                                            @ExceptionHandler(IllegalArgumentException.class)
                                            ResponseEntity<Map<String, String>> bad(IllegalArgumentException e) {
                                                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                                            }
                                        }
                                        """,
                                "The UI highlights the field. The log still has 400."),
                Concepts.of("Bean lifecycle",
                                "Construct, inject, @PostConstruct, use, @PreDestroy. You do not new a bean and call initialize by hand. Prototype beans are not a cache.",
                                "A Kafka consumer started in a constructor before the broker property was bound. Moving start to @PostConstruct (and better, SmartLifecycle) fixed a race with properties.")
                        .depth("""
                                Spring constructs the bean, injects fields/constructor, then calls initializing callbacks. @PostConstruct and InitializingBean live here. Do not start threads in a constructor — collaborators may not be ready, and you will not be able to test without side effects. @PreDestroy and DisposableBean run on close of the context. SIGTERM should reach this if the JVM is shutting down cleanly.

                                Scopes: singleton is the default. request and session exist for web. prototype creates a new instance when asked — injecting a prototype into a singleton still captures one instance unless you use ObjectFactory. People expect prototype to mean 'new every call' and then wonder why it is not.

                                SmartLifecycle lets you start after the context is up and stop before beans die — better than @PostConstruct for listeners. In interviews, order the callbacks and explain singleton versus prototype injection.
                                """)
                        .qa("Why not start a consumer in the constructor?",
                                "The object is not fully injected, properties may not be bound, and tests cannot construct the class without starting Kafka. Use lifecycle callbacks.",
                                "When does @PreDestroy run?")
                        .qa("You inject a prototype into a singleton. How many instances?",
                                "One, captured at injection, unless you look up via ObjectFactory each time. Prototype is not a per-call cache on a field.",
                                "What is SmartLifecycle for?")
                        .sample("Init after inject",
                                """
                                        @PostConstruct
                                        void validate() {
                                            Objects.requireNonNull(clock, "clock");
                                        }
                                        """,
                                "Constructor already received clock; this is for checks that need the full graph."),
                Concepts.of("Validation",
                                "Bean Validation belongs at the edge: @Valid on a request body, constraints on properties. Re-validating the same object in four layers is noise; skipping it is an incident.",
                                "An apply request with a blank jobId reached mail send. @NotBlank on the DTO plus MethodArgumentNotValidException advice stopped it at 400.")
                        .depth("""
                                Jakarta Bean Validation is annotations plus a validator. Boot wires it to MVC: @Valid or @Validated on a @RequestBody triggers MethodArgumentNotValidException. Field errors belong in a problem+json body, not a stack trace. @Validated on a @ConfigurationProperties bean fails startup — different and just as important.

                                Do not validate only in the UI. Do not validate only in the database. The HTTP edge is the contract for a public API. Domain invariants (money cannot be negative) belong on the domain type's constructor as well; annotations on a DTO are not a substitute for a record compact constructor.

                                Groups exist; most apps never need them. In interviews, name the annotation, the exception, and where properties validation runs.
                                """)
                        .qa("Who throws MethodArgumentNotValidException?",
                                "MVC, when @Valid fails on a request body. Your @ControllerAdvice turns it into 400 with field names.",
                                "How is that different from properties validation?")
                        .qa("Is @NotNull on a JPA entity enough?",
                                "It is a last line, not the contract. The API should reject first. The constructor should reject. The column constraint is the database's opinion.",
                                "When do you use @Validated versus @Valid?")
                        .sample("Edge DTO",
                                """
                                        public record ApplyRequest(@NotBlank String jobId, @Email String to) {}
                                        """,
                                "Controller method takes @Valid @RequestBody ApplyRequest."),
                Concepts.of("TaskExecutor and @Async",
                                "Background work needs a pool you own, with a size and a rejection policy. @Async without an executor uses a simple default that will surprise you under load.",
                                "Mail send ran @Async on the default executor and starved MVC during a burst. A dedicated TaskExecutor with a bound queue isolated the blast.")
                        .depth("""
                                ThreadPoolTaskExecutor is a pool with core size, max, queue, and a rejection policy. CallerRunsPolicy turns overflow into backpressure on the caller. AbortPolicy throws. Discard is how you lose mail. Name the pool. Set a thread name prefix so dumps are readable.

                                @Async proxies the bean (same self-invocation trap). You must enable it. The default executor is not sized for your traffic. Always provide a bean and point @Async at it. Virtual threads can back an executor for blocking tasks; they still need a bound on downstream connections.

                                Scheduling (@Scheduled) is another pool. Do not let a long tick overlap unless you said so. In interviews, size the pool, name the rejection policy, and mention the proxy.
                                """)
                        .qa("Why not @Async with zero configuration?",
                                "The default executor is not your capacity plan. Provide a named TaskExecutor and a rejection policy.",
                                "What does CallerRunsPolicy do?")
                        .qa("Does @Async skip the same self-invocation trap as @Transactional?",
                                "Yes. this.send() is synchronous. Call through the proxy.",
                                "When would you use virtual threads on an executor?")
                        .sample("Named pool",
                                """
                                        @Bean(name = "mailExecutor")
                                        TaskExecutor mailExecutor() {
                                            var pool = new ThreadPoolTaskExecutor();
                                            pool.setThreadNamePrefix("mail-");
                                            pool.setCorePoolSize(2);
                                            pool.setMaxPoolSize(4);
                                            pool.setQueueCapacity(100);
                                            pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                                            pool.initialize();
                                            return pool;
                                        }
                                        """,
                                "Mail cannot eat the request threads.")
        );
    }
}
