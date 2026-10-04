package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class SpringSecurityCatalog {

    private SpringSecurityCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("spring-security", "Spring Security",
                "Security is a filter chain and a decision. OAuth2, sessions, and method security are flavors of the same idea.",
                Concepts.of("SecurityFilterChain",
                                "The chain is the product. authorizeHttpRequests plus one authentication mechanism. Two chains if actuator lives on another port.",
                                "A personal classroom can stay permitAll. The moment it is on a network, this chain is the first PR — named paths, not a custom filter you cannot debug.")
                        .depth("""
                                Spring Security is a stack of servlet filters. SecurityFilterChain is the bean that orders them and sets authorization rules. authorizeHttpRequests is the matcher list: more specific first, anyRequest last. Authentication is a different knob (form, JWT, none). Mixing ad-hoc filters with the chain is how 403s become undebuggable.

                                permitAll is a choice, not a default you should forget. CSRF is on by default for browser sessions. TRACE logging of org.springframework.security beats shuffling filters at random.

                                Two chains exist when you need different rules for /actuator/** or a management port. In interviews, draw the chain, name authorizeHttpRequests, and say where a JWT filter sits.
                                """)
                        .qa("Filter versus interceptor versus advice?",
                                "Security filters wrap the whole HTTP exchange before the DispatcherServlet. MVC interceptors see the handler. Advice sees exceptions.",
                                "How do you have a public /api/lesson.json and a locked /admin?")
                        .qa("Where do you put actuator?",
                                "A second chain matching /actuator/**, or a separate port. Health must not require a user session if kube needs it.",
                                "What is SecurityContextHolder?")
                        .sample("Explicit rules",
                                """
                                        http.authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health").permitAll()
                                                .anyRequest().permitAll());
                                        """,
                                "Name the paths. Tighten when this leaves the laptop."),
                Concepts.of("Authentication versus authorization",
                                "Authentication answers who. Authorization answers whether that who may do this. Mixing them produces 401 when you meant 403, and logs that lie.",
                                "A logged-in demo user hitting /apply/run should be 403, not bounced to a login page as 401. The chain distinguished anonymous from authenticated-but-denied.")
                        .depth("""
                                Anonymous is a principal too in Spring — that surprises people. Authenticated means a real identity. Authorized means a voter or an authorize matcher allowed the action. ExceptionTranslationFilter turns AuthenticationException into 401 and AccessDeniedException into 403 for APIs.

                                Method security and URL security both authorize. URL security is coarse and cheap. Method security is the last line when a URL pattern is wrong. Do not skip URLs and hope annotations catch every entry point.

                                In interviews, define both in one sentence and pick the status code for each failure.
                                """)
                        .qa("401 or 403 for a logged-in user on /admin?",
                                "403. They are authenticated. 401 means we do not know who they are (or the token is unusable).",
                                "What does anonymous mean in Spring?")
                        .qa("Why both URL rules and @PreAuthorize?",
                                "URLs catch the obvious paths. Method security catches service entry from another controller you forgot to lock. Defense in depth, not duplication of every getter.",
                                "Where is the SecurityContext stored?")
                        .sample("API entry",
                                """
                                        http.authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/**").authenticated()
                                                .anyRequest().permitAll());
                                        """,
                                "Classroom currently permitAll; this is the networked shape."),
                Concepts.of("OAuth2 resource server",
                                "JWT in, Authentication out. You validate issuer and audience. You do not write your own parser.",
                                "A gateway in front of internal APIs trusted Keycloak tokens and rejected a token meant for another app because audience was checked.")
                        .depth("""
                                A resource server does not log you in. It accepts a bearer token and builds an Authentication. Spring's oauth2ResourceServer().jwt() uses an issuer's JWK set. Validate iss, aud, exp. Skipping aud is how a token for the blog admin works on the payroll API.

                                Scopes and roles land as authorities if you map them. Do not parse the JWT with a random library and stuff strings into a filter. Clock skew of a minute is normal; clock skew of an hour is a misconfigured NTP.

                                Confidential clients and public clients are different (see Keycloak). The resource server cares about the access token, not the refresh token. In interviews, say issuer, audience, JWK, no custom crypto.
                                """)
                        .qa("Why not parse JWT with a regex?",
                                "Signatures, issuer, audience, and expiry are the product. A library plus Spring's resource server already does this. Homegrown parsers skip aud.",
                                "What is a JWK set?")
                        .qa("Where does the refresh token live?",
                                "Not on the resource server. The client (or BFF) refreshes. APIs see short access tokens.",
                                "What happens if you skip audience?")
                        .sample("JWT resource server",
                                """
                                        http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
                                        """,
                                "Issuer URI lives in application.yml."),
                Concepts.of("Method security",
                                "@PreAuthorize is the last line of defense when a URL pattern is wrong. Use it on services, not on every getter.",
                                "An apply-queue endpoint must not run for a demo user even if someone guesses the path. The service method carried the rule.")
                        .depth("""
                                Enable method security. @PreAuthorize uses SpEL against the Authentication. hasRole('ADMIN') looks for ROLE_ADMIN unless you change the prefix. @Secured is the older cousin. @PreFilter/@PostFilter are easy to abuse on large collections.

                                Put it on the application service, the thing every controller must call. Putting it only on the controller misses a second mapping. Putting it on a helper used internally may never run if called via this.

                                SpEL in annotations is hard to test if it is clever. Keep expressions boring. In interviews, say enablement, role prefix, and the proxy.
                                """)
                        .qa("hasRole('ADMIN') looks for which authority?",
                                "ROLE_ADMIN by default. hasAuthority('ADMIN') looks for ADMIN. Mixing them is a 403 that looks like a matcher bug.",
                                "Why on the service, not the getter?")
                        .qa("Does @PreAuthorize work on a private method?",
                                "No usefully — the proxy does not intercept private calls. Public methods on a Spring bean.",
                                "How do you unit-test the expression?")
                        .sample("Service guard",
                                """
                                        @PreAuthorize("hasRole('ADMIN')")
                                        public void runApplyQueue() {
                                            queue.drain();
                                        }
                                        """,
                                "A guessed URL is not enough."),
                Concepts.of("CSRF and browsers",
                                "Browser sessions need CSRF. Bearer-token APIs usually disable it. Mixing the two is how forms mysteriously 403.",
                                "Thymeleaf POSTs need the token. A JSON GET for lesson.json does not. This classroom keeps CSRF defaults and uses GET for json and zip.")
                        .depth("""
                                CSRF is a browser trick: the other site causes your session cookie to be sent. A synchronizer token on mutating requests stops it. GET must stay safe — never mutate on GET, including generate-if you later change it to POST.

                                Bearer tokens in Authorization are not sent automatically by foreign sites the same way cookies are, so APIs often csrf.disable() for those matchers. Cookie-based SPAs need CSRF or SameSite plus careful CORS. Do not disable CSRF globally because a JSON POST 403'd — fix the client.

                                Spring's defaults are on. This app's lesson, skills, generate, and zip are GET, so CSRF is not in the way. In interviews, say when you disable it and when you must not.
                                """)
                        .qa("Why did my form POST 403?",
                                "Missing CSRF token on a session-authenticated form. Add the hidden field or header. Do not disable CSRF to 'fix' it.",
                                "Why are lesson.json and project.zip GET?")
                        .qa("When do you disable CSRF?",
                                "For stateless Bearer APIs, on those matchers, on purpose. Not for cookie-session Thymeleaf apps.",
                                "Is SameSite a CSRF replacement?")
                        .sample("Keep defaults",
                                """
                                        http.csrf(Customizer.withDefaults());
                                        """,
                                "GET APIs work. Forms still protected."),
                Concepts.of("Password and session hygiene",
                                "DelegatingPasswordEncoder, HttpOnly cookies, short sessions. Never log tokens. Never store raw passwords.",
                                "A forgotten-password flow stored a hash, not the reset token in server logs. Session cookies were HttpOnly and Secure in prod.")
                        .depth("""
                                PasswordEncoderFactories.createDelegatingPasswordEncoder() prefixes the hash so you can rotate algorithms. {noop} is for tests, not prod. Sessions: timeout, fixation protection (Spring does this on login), HttpOnly, Secure, SameSite. Do not put JWTs in localStorage if you can use an HttpOnly cookie BFF.

                                Refresh tokens, reset tokens, and session ids are secrets. Logs, metrics tags, and support tickets leak them. Truncate if you must correlate.

                                In interviews, say delegating encoder, cookie flags, and one leak you would hunt in logs.
                                """)
                        .qa("Why a delegating password encoder?",
                                "So you can upgrade hashes without a flag day. The prefix names the algorithm. New logins rehash.",
                                "Where do you store a password reset token?")
                        .qa("Why HttpOnly on a session cookie?",
                                "JavaScript cannot read it, which cuts a class of XSS theft. XSS can still cause requests; CSRF tokens still matter.",
                                "Should you log Authorization?")
                        .sample("Encoder bean",
                                """
                                        @Bean
                                        PasswordEncoder passwordEncoder() {
                                            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
                                        }
                                        """,
                                "Tests may use {noop}; production never does."),
                Concepts.of("CORS",
                                "CORS is a browser isolation feature, not an authentication mechanism. Allowing * with credentials is a contradiction. APIs used by non-browsers do not care.",
                                "A SPA on another origin failed to download the studio zip until Access-Control-Expose-Headers included Content-Disposition — after the team stopped using * with cookies.")
                        .depth("""
                                A CORS preflight is an OPTIONS request. Spring's CorsConfiguration names origins, methods, headers, and whether credentials are allowed. allowCredentials(true) cannot pair with origin *. List origins. Expose headers the browser JS must read.

                                CORS does not stop curl. Authorization still must run. A public permitAll API can still send CORS headers for a browser classroom.

                                In interviews, distinguish CORS from CSRF and from authz, and say why * plus cookies is illegal in the spec.
                                """)
                        .qa("Does CORS protect your API from Postman?",
                                "No. It is a browser rule. Authentication and authorization still apply.",
                                "Why not allowedOriginPattern * with cookies?")
                        .qa("Why expose Content-Disposition?",
                                "JS on another origin cannot read that header on a download unless you expose it. Same-origin classroom pages do not need it.",
                                "What is a preflight?")
                        .sample("Tight CORS",
                                """
                                        @Bean
                                        CorsConfigurationSource cors() {
                                            var config = new CorsConfiguration();
                                            config.setAllowedOrigins(List.of("https://studio.example"));
                                            config.setAllowedMethods(List.of("GET", "HEAD"));
                                            config.setAllowCredentials(true);
                                            var source = new UrlBasedCorsConfigurationSource();
                                            source.registerCorsConfiguration("/api/**", config);
                                            return source;
                                        }
                                        """,
                                "List origins. Do not star them."),
                Concepts.of("SecurityContext across threads",
                                "SecurityContextHolder defaults to a ThreadLocal. @Async, Kafka listeners, and virtual-thread hops will not see the user unless you propagate it.",
                                "An @Async mail sender dropped the user and sent as anonymous. A DelegatingSecurityContextAsyncTaskExecutor wrapped the pool.")
                        .depth("""
                                MODE_THREADLOCAL is the default. A new thread starts empty. Spring Security offers MODE_INHERITABLETHREADLOCAL and wrapping executors. Reactor has its own context. Virtual threads are still threads — a jump to a pool without wrapping loses the context.

                                Do not stash Authentication in a static. Pass the user id as a method argument into async work if that is clearer. For HTTP, the context is set by the filter and cleared after the request.

                                In interviews, name ThreadLocal, the async executor wrapper, and why a Kafka listener is not the web user.
                                """)
                        .qa("Why is SecurityContext empty in @Async?",
                                "New thread, empty ThreadLocal. Wrap the executor or pass the user id as data.",
                                "Should a Kafka listener reuse the web Authentication?")
                        .qa("Is INHERITABLETHREADLOCAL enough?",
                                "It helps child threads created after the context is set, not pooled workers that were started at boot. Wrapping the pool is explicit.",
                                "Where is the context cleared?")
                        .sample("Wrap the pool",
                                """
                                        @Bean
                                        AsyncTaskExecutor securedMail(TaskExecutor mailExecutor) {
                                            return new DelegatingSecurityContextAsyncTaskExecutor(mailExecutor);
                                        }
                                        """,
                                "Or pass userId as a plain argument — often clearer.")
        );
    }
}
