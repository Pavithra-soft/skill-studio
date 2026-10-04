package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class SpringBootCatalog {

    private SpringBootCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("spring-boot", "Spring Boot",
                "Spring Boot is how Java teams ship. The ideas that survive every version bump are auto-config, typed properties, honest health, slices, and a jar that dies cleanly.",
                Concepts.of("Auto-configuration",
                                "Boot wires beans from the classpath so you do not hand-build DataSource and KafkaTemplate in every app. Your job is to know what it turned on.",
                                "A service that suddenly opened Redis was a starter on the classpath plus a host property, not magic. The conditions report named the class in one glance.")
                        .depth("""
                                Auto-configuration is a set of @Configuration classes gated by conditions: @ConditionalOnClass, OnMissingBean, OnProperty, OnWebApplication. Boot collects them from META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports. Your application class is not special — it is another configuration. The conditions report (--debug or /actuator/conditions) is the source of truth. Guessing is how you get two DataSources.

                                Fighting auto-config by copying a @Bean you do not understand duplicates the infrastructure. Prefer @ConditionalOnMissingBean when you replace a clock or a client so tests can override. Exclude an auto-configuration only when the starter must stay for other bits. Removing the starter is cleaner when you did not want the feature at all.

                                When you ship a library, you write auto-configuration. When you ship one app, a @Configuration is enough. @SpringBootApplication is @Configuration plus component scan plus enable-auto-config. In interviews, walk from a starter on the classpath to the bean in the context without waving at 'magic'.
                                """)
                        .qa("A Redis connection appeared. How do you find why?",
                                "Read the conditions report. A starter plus a host property is almost always the answer. Exclude the auto-config or remove the starter if you did not want it.",
                                "What is the difference between exclude and @ConditionalOnMissingBean?")
                        .qa("When do you write your own auto-configuration?",
                                "When you ship a library to several apps. Inside one app, a @Configuration is enough. The imports file is for starters.",
                                "How would you test an auto-config class?")
                        .sample("Override only when missing",
                                """
                                        @Bean
                                        @ConditionalOnMissingBean
                                        Clock clock() {
                                            return Clock.systemUTC();
                                        }
                                        """,
                                "Tests can provide a fixed Clock without excluding a starter."),
                Concepts.of("Typed configuration properties",
                                "@ConfigurationProperties beats a pile of @Value fields. You get validation, metadata, and a single object to pass around.",
                                "IMAP host, folder, and poll interval belonged in one MailProperties bean so three services could not drift. A missing host failed Boot startup instead of the first poll.")
                        .depth("""
                                Bind a prefix to a bean you own. Records work on current Boot when the canonical constructor matches the properties. @Validated plus @NotBlank fails fast at startup — that is the point. Relaxed binding maps GEMINI_API_KEY, gemini.api-key, and gemini.apiKey to the same field. Nested types keep mail.host next to mail.folder.

                                @Value is a magnet for duplication. A rename becomes a scavenger hunt. Properties classes are the document of what the app accepts. Enable them with @EnableConfigurationProperties or @ConfigurationPropertiesScan. Nested maps and lists bind; unknown keys can fail with ignore-unknown set to false in tests if you want tightness.

                                Do not inject Environment and string-key your way through production. Do not log the bound object if it contains secrets. Metadata JSON (the annotation processor) powers IDE completion — optional, but a sign you treated config as API. In interviews, contrast @Value with a validated properties record.
                                """)
                        .qa("Why not @Value everywhere?",
                                "No grouping, no metadata, no validation of the whole object, and a rename is a scavenger hunt. Properties classes document what the app accepts.",
                                "How does relaxed binding map env vars?")
                        .qa("How do you fail fast on a missing IMAP host?",
                                "@NotBlank on the field, @Validated on the properties bean, and a test that Boot refuses to start. Do not discover it on the first poll.",
                                "Where do you put the metadata JSON?")
                        .sample("Grouped mail settings",
                                """
                                        @ConfigurationProperties(prefix = "app.mail")
                                        public record MailProperties(@NotBlank String host, String folder, int pollSeconds) {}
                                        """,
                                "One object passed to the service. Startup fails if host is blank."),
                Concepts.of("Actuator readiness versus liveness",
                                "Liveness is 'the JVM is not deadlocked'. Readiness is 'dependencies will take traffic'. Kubernetes needs both. Pointing both at the same /health turns a slow Kafka reconnect into a crash loop.",
                                "During a rolling deploy, kube-proxy kept sending traffic until Kafka was reachable because readiness used the same check as liveness. Splitting the probes stopped 502s on the first hundred requests.")
                        .depth("""
                                A liveness failure kills the pod. A readiness failure only removes it from the Service endpoints. If Kafka is reconnecting, you want the second, not the first. Boot's separate groups exist for this: /actuator/health/liveness and /actuator/health/readiness when probes are enabled. Show the component that failed in the body so on-call does not guess.

                                Health indicators must be cheap. Do not run a full search from /health. Do not wait on a lock. Database ping and a Kafka cluster-id check are typical. A custom HealthIndicator is a bean; keep it boring. On a public network, actuator is not anonymous — lock it down or put it on another port.

                                Mixing probes is the classic Kubernetes outage. Liveness that includes the database will kill pods during a brief failover, which makes the failover worse. Readiness that never includes the database will take traffic and 500. In interviews, define both in one sentence and name the HTTP paths.
                                """)
                        .qa("Why split liveness and readiness?",
                                "Readiness fail removes you from the Service. Liveness fail kills the pod. Killing a pod that is only waiting on Kafka makes the outage worse.",
                                "What should readiness check in a Boot API?")
                        .qa("How do you add a custom indicator?",
                                "HealthIndicator beans. Keep them cheap. Do not run a full search from health.",
                                "Should /actuator be public on the internet?")
                        .sample("Probe flags",
                                """
                                        management.endpoint.health.probes.enabled=true
                                        management.endpoints.web.exposure.include=health,info
                                        """,
                                "Kube hits /actuator/health/readiness. Screen only."),
                Concepts.of("Test slices",
                                "@WebMvcTest and @DataJpaTest boot a sliver of the context. Full @SpringBootTest is for the last mile, not every class.",
                                "Controller tests that did not start Kafka cut a forty-second suite down to seconds, so people actually ran them before pushing.")
                        .depth("""
                                A slice test loads the layer you are changing. @WebMvcTest starts MVC (and Security if present), not your Kafka consumers. @DataJpaTest starts the persistence slice. @JsonTest is for serializers. @SpringBootTest starts the world: useful for wiring, Flyway, a real servlet container in RANDOM_PORT tests. Suites that always boot the world stop being run.

                                Mock the neighbors, not the class under test. @MockitoBean (or @MockBean on older Boot) replaces a bean. @TestConfiguration can provide a fixed Clock. Do not hit a paid API from a unit test. Assert status, headers, and JSON, not log lines.

                                Slice tests miss wiring bugs. Keep a small number of full tests: one that boots, one that migrates, one that hits actuator health. That mix is the senior default. In interviews, say which annotation, which beans are in, and why forty-second tests are a process failure.
                                """)
                        .qa("When is @SpringBootTest the wrong default?",
                                "When you are asserting a controller status or a repository query. Slice first. One or two full tests for the graph.",
                                "What beans does @WebMvcTest include?")
                        .qa("How do you fake a gateway in a web slice?",
                                "@MockitoBean or a @TestConfiguration @Bean of the same type. Assert the HTTP contract, not the collaborator's internals.",
                                "Why is MockMvc still useful on a full test?")
                        .sample("Controller slice",
                                """
                                        @WebMvcTest(HomeController.class)
                                        class HomeControllerTest {
                                            @Autowired MockMvc mvc;
                                        }
                                        """,
                                "Kafka stays off. The request path is still real."),
                Concepts.of("Profiles and external config",
                                "dev, prod, and secret-bearing env vars. Never commit a real API key. The same jar runs on a laptop and on Render because URLs come from the environment.",
                                "A GEMINI key rotation did not need a rebuild once the property came from the env. The laptop still used an optional .env import.")
                        .depth("""
                                Spring profiles activate groups of beans and documents: application-prod.yml overlays the base. SPRING_PROFILES_ACTIVE is the operator's lever. Do not encode prod behavior in if (hostname.contains(\"prod\")). Cloud platforms inject env vars; Boot's relaxed binding maps them. optional:file:.env[.properties] is a laptop convenience, not a production secret store.

                                Precedence matters: command line, env, application-{profile}.yml, application.yml. Fight it and you will debug the wrong file. Spring Cloud's extra sources are out of scope here — Boot externalization is already the 12-factor rule.

                                Secrets never belong in the image. Render, Kubernetes, and your laptop disagree on where they live; the jar should not care. In interviews, walk a property from an env var to a @ConfigurationProperties field, and say what you refuse to commit.
                                """)
                        .qa("How does GEMINI_API_KEY become a property?",
                                "Relaxed binding. GEMINI_API_KEY, gemini.api-key, and gemini.apiKey meet the same field. Env beats YAML.",
                                "Where should production secrets live?")
                        .qa("When do you use a profile versus an env var?",
                                "Profiles for shape (which beans, which log levels). Env vars for instance values (hosts, keys). Do not create a profile per region if env is enough.",
                                "What is the precedence order?")
                        .sample("Laptop import",
                                """
                                        spring.config.import=optional:file:.env[.properties]
                                        """,
                                "Optional so CI without a .env still starts."),
                Concepts.of("Starters",
                                "A starter is a curated classpath plus auto-configuration. You depend on spring-boot-starter-web, not on four servlet jars you chose by hand.",
                                "Adding spring-boot-starter-data-jpa because a tutorial said so pulled Hibernate into an app that only needed a Redis cache. Removing the starter deleted a class of connection errors.")
                        .depth("""
                                Starters exist so versions align. The Boot BOM (the parent POM) manages those versions. Mixing a random Jackson version with starter-web is how you get method-not-found at 2am. Prefer the BOM. If you need a library Boot does not manage, pin it and write down why.

                                Each starter turns on a slice of auto-config. starter-web is Tomcat, Jackson, validation glue. starter-actuator is health. starter-security is the filter chain. You can exclude Tomcat and use Jetty; that is a documented swap, not a hobby.

                                A custom starter for your company is appropriate when several apps share the same Kafka plus tracing setup. Inside one app, a module is enough. In interviews, name three starters you actually use and what they bring onto the classpath.
                                """)
                        .qa("Why not pick Hibernate and Jackson versions yourself?",
                                "The Boot BOM already did. Drift is how NoSuchMethodError shows up in production. Override only with a comment.",
                                "How do you swap Tomcat for Jetty?")
                        .qa("When do you write a company starter?",
                                "When several apps share the same auto-config. One app should use a @Configuration, not a starter for fashion.",
                                "What is the difference between a starter and a library?")
                        .sample("Parent BOM",
                                """
                                        <parent>
                                            <groupId>org.springframework.boot</groupId>
                                            <artifactId>spring-boot-starter-parent</artifactId>
                                            <version>3.5.5</version>
                                        </parent>
                                        """,
                                "Versions align. Screen only."),
                Concepts.of("Logging and MDC",
                                "Logs are the product on-call reads. A correlation id in the MDC turns a user complaint into a grep. Logging payloads with secrets is a security incident.",
                                "A user said 'it failed at 4pm'. Without an X-Correlation-Id on every line from the filter to the Kafka listener, the team guessed. One MDC put in a filter made the apply traceable.")
                        .depth("""
                                SLF4J is the facade; Logback is Boot's default. Log at the layer that knows the meaning: the service logs 'apply failed for job X', the framework logs the stack. Do not log then rethrow at every layer. INFO is for business events, WARN for recoverable trouble, ERROR for pages. DEBUG is not a production default.

                                MDC (Mapped Diagnostic Context) is a thread-local map copied onto every line. Put a correlation id in a filter, clear it in a finally. Virtual threads and thread pools can leak MDC if you do not wrap the task. Micrometer tracing (Brave/OpenTelemetry) is the grown-up version; MDC is the minimum.

                                Never log tokens, passwords, or full resumes at INFO. JSON logs help production; pattern logs help laptops. In interviews, say where the id is set, where it is cleared, and why a payload log is a threat.
                                """)
                        .qa("Where do you set a correlation id?",
                                "A servlet filter (or gateway) on inbound HTTP, then copy it to Kafka headers and MDC. Clear in finally so a pooled thread does not leak.",
                                "What leaks MDC onto the wrong request?")
                        .qa("INFO versus ERROR?",
                                "INFO is an expected business event. ERROR pages a human. Logging every 404 at ERROR trains people to ignore logs.",
                                "Why not log the Authorization header at DEBUG in prod?")
                        .sample("Filter puts the id",
                                """
                                        MDC.put("corr", Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                                                .orElse(UUID.randomUUID().toString()));
                                        try {
                                            chain.doFilter(request, response);
                                        } finally {
                                            MDC.remove("corr");
                                        }
                                        """,
                                "Every log line in that request carries corr."),
                Concepts.of("Graceful shutdown",
                                "SIGTERM should finish in-flight requests and stop taking new ones. A kill -9 deploy drops applies. Boot can wait, but only if the platform sends the signal and you set a timeout.",
                                "Kubernetes sent SIGTERM, then killed the pod five seconds later while Boot was still draining HTTP. Matching terminationGracePeriodSeconds to server.shutdown=graceful stopped truncated cover letters.")
                        .depth("""
                                server.shutdown=graceful tells Tomcat to stop accepting and wait for active requests, up to spring.lifecycle.timeout-per-shutdown-phase. Actuator's liveness can stay up while readiness goes down if you wire it that way — even better, let the Service endpoints drain via readiness first, then SIGTERM.

                                The process must be PID 1 or a process manager that forwards signals. A shell-form Docker ENTRYPOINT swallows SIGTERM. exec form java -jar is the default for a reason. In-flight JDBC work still needs the database to stay reachable during the grace period.

                                Kafka consumers should wake and commit. Schedulers should not start a new tick. In interviews, walk SIGTERM from kubelet to Tomcat to your listener, and name the two timeouts that must agree.
                                """)
                        .qa("What does graceful shutdown actually wait for?",
                                "In-flight HTTP requests, up to the lifecycle timeout. It does not wait forever, and it does not finish a four-minute job unless you said so.",
                                "Why does shell-form ENTRYPOINT break this?")
                        .qa("Readiness versus SIGTERM order?",
                                "Fail readiness so the Service stops sending traffic, then terminate. If you kill first, you still get 502s.",
                                "How do you pick terminationGracePeriodSeconds?")
                        .sample("Boot drain",
                                """
                                        server.shutdown=graceful
                                        spring.lifecycle.timeout-per-shutdown-phase=20s
                                        """,
                                "Match this to the pod's grace period.")
        );
    }
}
