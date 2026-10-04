package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class DockerCatalog {

    private DockerCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("docker", "Docker CLI",
                "Docker is a packaging contract. The image is the unit you promote. Layers and a non-root user are the habits that stick.",
                Concepts.of("Image layers",
                                "Put slow, stable layers first. Copy the jar last. That is why rebuilds stay quick.",
                                "A one-line Java change should not re-download the JDK layer. COPY pom.xml then src, or a multi-stage Maven cache, keeps CI tolerable.")
                        .depth("""
                                Each Dockerfile instruction is a layer. Docker reuses a layer when its instruction and parent are unchanged. COPY . . as the first step invalidates everything on any file change. Copy the manifest, resolve dependencies, then copy sources.

                                Layer count is not a trophy. Combining RUN apt-get with cleanup in the same layer keeps the image smaller because deleted files never survive to the next layer.

                                In interviews, order a Boot Dockerfile from parent image to jar.
                                """)
                        .qa("Why copy pom.xml before src?",
                                "Dependency layers cache until the POM changes. Source changes then only rebuild the last steps.",
                                "Why apt-get clean in the same RUN?")
                        .qa("Does every RUN make the image slower?",
                                "Not if they cache. Too many tiny layers make push/pull chatty; combining unrelated steps hurts cache. Balance.",
                                "What invalidates a layer?")
                        .sample("Jar last",
                                """
                                        COPY target/skill-studio-1.0.0.jar app.jar
                                        """,
                                "The JDK layer stays cached."),
                Concepts.of("One process, one container",
                                "PID 1 should be your app, with a proper signal handler. Sidecars are extra containers, not extra processes stuffed in.",
                                "SIGTERM from Kubernetes must reach Spring Boot so in-flight applies finish. A shell wrapper swallowed the signal.")
                        .depth("""
                                exec-form ENTRYPOINT ["java","-jar","app.jar"] makes Java PID 1. Shell form java -jar is a shell that may not forward SIGTERM. Dumb-init or a proper runtime is the alternative if you must wrap.

                                A second process (nginx plus java in one container) means your orchestrator's probes and restarts cannot target one of them. Compose services and k8s pods with two containers exist for that.

                                In interviews, PID 1, signals, and why not bash -c.
                                """)
                        .qa("Why exec form?",
                                "No wrapping shell. SIGTERM reaches the JVM. Boot graceful shutdown can run.",
                                "What is PID 1's job?")
                        .qa("Can you run cron and the app together?",
                                "You can, and you own a process manager now. Prefer a CronJob plus a Deployment.",
                                "Sidecar versus extra process?")
                        .sample("Entrypoint",
                                """
                                        ENTRYPOINT ["java", "-jar", "app.jar"]
                                        """,
                                "Signals reach Boot."),
                Concepts.of("Non-root user",
                                "Root in a container is still a bad habit. A numeric USER is what scanners expect.",
                                "A CVSS write-up about a crafted image is why you do not run Boot as root, even on a laptop demo that later goes to Render.")
                        .depth("""
                                USER 1000 (or a named user you created) drops privileges for the running process. Bind-mount permissions must match. Numeric ids are more portable than names when the image has no /etc/passwd entry.

                                Running as root makes a container escape more interesting. Distroless images often have a non-root user built in. Writeable dirs must be chowned in the Dockerfile before you drop.

                                In interviews, USER, numeric id, and a volume permission pitfall.
                                """)
                        .qa("Why numeric USER?",
                                "The runtime may not have the name. Kubernetes securityContext.runAsUser matches a number.",
                                "What breaks if the jar directory is root-owned?")
                        .qa("Is root OK in the build stage?",
                                "Yes. Multi-stage: compile as whatever, copy the jar, run as non-root.",
                                "What is a distroless image?")
                        .sample("Drop privileges",
                                """
                                        RUN useradd --create-home --shell /usr/sbin/nologin app
                                        USER app
                                        """,
                                "chown what you must write first."),
                Concepts.of("Health at the image edge",
                                "HEALTHCHECK is for Docker Compose. Kubernetes uses probes. Do not assume they are the same.",
                                "Compose on a laptop restarted a wedged container via HEALTHCHECK. The cluster used readiness and ignored the Dockerfile's HEALTHCHECK.")
                        .depth("""
                                Dockerfile HEALTHCHECK runs a command inside the container. Compose maps it to a restart policy. Kubernetes does not use it for Service endpoints — you must set probes on the Pod spec.

                                Hitting /actuator/health from HEALTHCHECK is fine for Compose. Keep it cheap. curl may not exist in a JRE image; wget or a tiny Java call, or install curl in a debug image only.

                                In interviews, split Compose and kube, and name the Boot path.
                                """)
                        .qa("Will Kubernetes honor HEALTHCHECK?",
                                "Not as a Service readiness signal. Define probes. HEALTHCHECK is still useful for Compose and some PaaS.",
                                "What if curl is missing?")
                        .qa("Which URL?",
                                "Readiness-style: can we take traffic. A liveness HEALTHCHECK that includes the DB will restart during a blip.",
                                "How often should it run?")
                        .sample("Compose-oriented",
                                """
                                        HEALTHCHECK --interval=30s --timeout=3s \\
                                          CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1
                                        """,
                                "JRE images may need a different command."),
                Concepts.of("Tagging and promotion",
                                "latest is not a version. Promote digest-pinned images through environments.",
                                "Prod must not float to a new latest while you sleep. A sha256 pin plus a git tag is the trail.")
                        .depth("""
                                Tags are mutable pointers. latest moved under you. Digest (image@sha256:…) is immutable. Promote the same digest from staging to prod. Git SHA tags (skill-studio:git-abc123) are a human handle; still pin digest in prod manifests.

                                Never retag over a shipped version. Cut 1.0.1. Cleanup dangling images in CI, not by hand on a laptop as policy.

                                In interviews, mutable tags versus digests, and a promotion path.
                                """)
                        .qa("Why is latest dangerous in prod?",
                                "A pull can change bytes without a deploy record. Pin digest or an immutable tag you never overwrite.",
                                "How do you promote?")
                        .qa("Is a git SHA tag enough?",
                                "Better than latest. If someone force-pushes the tag, it is not enough. Digest is the lock.",
                                "Where do you record the digest?")
                        .sample("Digest pin",
                                """
                                        image: ghcr.io/example/skill-studio@sha256:0123456789abcdef
                                        """,
                                "The bytes cannot float."),
                Concepts.of("Multi-stage builds",
                                "Compile in a Maven image, copy the jar into a JRE image. The compiler and .m2 cache stay out of production.",
                                "The Render-ready Dockerfile builds with maven:3.9-eclipse-temurin-21 and runs on eclipse-temurin:21-jre.")
                        .depth("""
                                FROM ... AS build, then FROM a slimmer runtime, COPY --from=build. You get a smaller attack surface and a smaller pull. Do not copy the whole target directory if a fat jar is enough.

                                Caching Maven deps in CI (BuildKit cache mounts) is an optimization, not required for correctness.

                                In interviews, two FROMs and what must not land in the final image.
                                """)
                        .qa("What stays out of the runtime image?",
                                "JDK compilers if you only need a JRE, Maven, source, tests, and .git. The fat jar and a JRE are enough for this app.",
                                "Why not run Maven in prod?")
                        .qa("COPY --from=build copies what?",
                                "Files from the named stage's filesystem, not from your laptop. Paths are in-container paths.",
                                "Can you have more than two stages?")
                        .sample("Two stages",
                                """
                                        FROM maven:3.9.11-eclipse-temurin-21 AS build
                                        # mvn package
                                        FROM eclipse-temurin:21-jre
                                        COPY --from=build /app/target/skill-studio-1.0.0.jar app.jar
                                        """,
                                "No Maven in the final image."),
                Concepts.of(".dockerignore",
                                "The build context is everything you send to the daemon. Without a ignore file you send .git, target, and secrets.",
                                "CI slowed because the context included .git and a local data/ directory. .dockerignore cut the upload to source and the POM.")
                        .depth("""
                                Docker sends the context before it even reads which files COPY needs, unless you use advanced ignore and sparse patterns. Ignore target/, .git, .idea, .env, data. If you COPY src, you still benefit because the upload is smaller.

                                .env in the context is a leak waiting for a mistaken COPY. Treat ignore like .gitignore for the daemon.

                                In interviews, context versus image, and a secret you would ignore.
                                """)
                        .qa("Does .gitignore replace .dockerignore?",
                                "No. The daemon does not read .gitignore. A .env ignored by git can still be in the context if a developer has it locally.",
                                "Why ignore target/?")
                        .qa("What is the build context?",
                                "The directory you pass to docker build, sent to the daemon. Smaller is faster and safer.",
                                "Can you COPY a file that is ignored?")
                        .sample("Ignore local junk",
                                """
                                        target/
                                        .git
                                        .idea
                                        .env
                                        .DS_Store
                                        """,
                                "Keep the context to source and pom.xml."),
                Concepts.of("Slim runtime images",
                                "A JRE or distroless image is enough for a fat jar. A full JDK plus Ubuntu debug tools is a larger attack surface.",
                                "eclipse-temurin:21-jre is the runtime this classroom Dockerfile uses. Debug with an ephemeral debug container, not by baking in curl forever.")
                        .depth("""
                                Distroless and JRE images omit shells and package managers. That is good for prod and annoying for live debug — use ephemeral debug pods or a debug tag you do not ship. Alpine plus musl has historically hurt some JVMs; Temurin Debian/Ubuntu JRE is the boring default.

                                CVE churn lives in the base image. Rebuild regularly. Pin by digest.

                                In interviews, JRE versus JDK, and how you debug without a shell.
                                """)
                        .qa("JDK or JRE in prod?",
                                "JRE (or a jlink runtime) unless you compile in the same image, which you should not.",
                                "How do you debug distroless?")
                        .qa("Why rebuild the base often?",
                                "OS CVEs. Your jar may be fine; the base is not frozen in time unless you pin and still rebuild.",
                                "Alpine JVM footguns?")
                        .sample("JRE runtime",
                                """
                                        FROM eclipse-temurin:21-jre
                                        """,
                                "Build stage can still use a full JDK.")
        );
    }
}
