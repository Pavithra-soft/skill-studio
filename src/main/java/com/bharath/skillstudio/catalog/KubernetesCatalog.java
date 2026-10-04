package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class KubernetesCatalog {

    private KubernetesCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("kubernetes", "Kubernetes",
                "Kubernetes is desired state. Pods die. Controllers put them back. You design for that, not against it.",
                Concepts.of("Pods are cattle",
                                "A pod is disposable. Anything you care about lives in a volume, a database, or an object store.",
                                "A replica can vanish. Postgres or a mounted volume must outlive it. Local H2 files in the container filesystem will not.")
                        .depth("""
                                A pod is a scheduling unit: one or more containers sharing network and volumes. The kubelet (and controllers) will kill it for node pressure, rolling updates, or a failed liveness probe. Your process must start empty and attach to durable state.

                                Sticky sessions and local disk caches are how people fight this and lose. Use a database, object storage, or a volume with a known lifecycle (PVC).

                                In interviews, 'the pod died, what survived?' is the question. Answer with data, not with 'we set restartPolicy'.
                                """)
                        .qa("Where does session state live?",
                                "A shared store, a JWT, or a sticky session you will regret. Not in the pod's memory if you have two replicas.",
                                "What survives a pod delete?")
                        .qa("Is a Deployment's pod cattle or a pet?",
                                "Cattle. Jobs and StatefulSets are still replaceable, they just have identity and volume claims.",
                                "Why not store uploads on the local disk?")
                        .sample("Image is the unit",
                                """
                                        spec:
                                          containers:
                                            - name: app
                                              image: skill-studio:1.0.0
                                        """,
                                "No precious files in the container layer."),
                Concepts.of("Probes",
                                "liveness kills a stuck JVM. readiness removes it from the Service. Mixing them makes a crash loop look like a network outage.",
                                "During Boot startup, the Service waited on /actuator/health/readiness instead of 502ing the first searches.")
                        .depth("""
                                startupProbe covers slow boots. livenessProbe answers 'should we kill this'. readinessProbe answers 'should we send traffic'. Pointing liveness at a dependency turns a Kafka blip into a restart storm.

                                HTTP GET on Boot's probe endpoints is the default. A command probe that deadlocks the event loop is a joke you will not get. Give startup enough time for Flyway and classloading.

                                In interviews, three probes, two Boot paths, one anti-pattern.
                                """)
                        .qa("Why not use the same URL for liveness and readiness?",
                                "A down dependency should not kill the JVM. Readiness fails; liveness stays up unless the process is wedged.",
                                "What is startupProbe for?")
                        .qa("What should readiness include?",
                                "Enough to serve: the DB if you cannot work without it. Not a full user journey.",
                                "How long should the first probe wait?")
                        .sample("HTTP readiness",
                                """
                                        readinessProbe:
                                          httpGet:
                                            path: /actuator/health/readiness
                                            port: 8080
                                        """,
                                "Enable Boot probes in application.yml."),
                Concepts.of("Deployments and rollouts",
                                "maxUnavailable and maxSurge are how you ship without a maintenance window. A bad image must roll back.",
                                "A config change that broke health checks reverted with kubectl rollout undo because the previous ReplicaSet still existed.")
                        .depth("""
                                A Deployment owns ReplicaSets. Rolling update starts new pods, then drops old ones, bounded by maxUnavailable and maxSurge. Readiness gates the cutover: unready new pods should not take traffic.

                                Recreate is a brief outage. It is simpler and sometimes honest for singletons. rollback depends on still having the previous ReplicaSet (revisionHistoryLimit).

                                In interviews, walk a rolling update and a rollback without a war room.
                                """)
                        .qa("What does maxUnavailable: 0 mean?",
                                "Do not drop old pods until new ones are ready (you will need surge capacity). Safer, needs extra nodes.",
                                "How do you undo a rollout?")
                        .qa("Why did users see 502 during deploy?",
                                "New pods were Ready before the app could serve, or old pods died before new ones passed readiness. Fix probes and surge.",
                                "Recreate versus RollingUpdate?")
                        .sample("Gentle roll",
                                """
                                        spec:
                                          strategy:
                                            rollingUpdate:
                                              maxUnavailable: 0
                                              maxSurge: 1
                                        """,
                                "Needs headroom."),
                Concepts.of("ConfigMaps and Secrets",
                                "Config is not the image. Secrets are not ConfigMaps. Mount them, do not bake them.",
                                "API keys rotated without rebuilding the jar once they came from a Secret env var.")
                        .depth("""
                                ConfigMaps are non-secret files and env. Secrets are base64 (not encryption — RBAC and encryption at rest are extra). Do not check either into git with real values. Do not put secrets in ConfigMaps because they were easier to kubectl apply.

                                Mount as files if you need updates without a restart (your app must watch). Env vars are simpler and require a rollout to change. Never log the values.

                                In interviews, rotation without a rebuild, and why base64 is not security.
                                """)
                        .qa("Is a Secret encrypted?",
                                "Not by default — it is base64 in etcd unless you enable encryption at rest. Treat RBAC as the door.",
                                "ConfigMap or Secret for a DB password?")
                        .qa("How do you rotate a key?",
                                "Update the Secret, rollout the Deployment so pods pick up env, or watch a mounted file. Do not bake the key in the image.",
                                "Why not git-ops a live secret in plaintext YAML?")
                        .sample("env from secret",
                                """
                                        envFrom:
                                          - secretRef:
                                              name: skill-studio
                                        """,
                                "Keys become env vars. Screen only."),
                Concepts.of("Resource requests",
                                "Requests are what the scheduler believes. Limits are the ceiling. CPU throttle feels like a mystery GC pause.",
                                "A Kafka consumer over-limited on CPU lagged, looked idle, then OOM'd in a burst when the limit was memory-only and the request was missing.")
                        .depth("""
                                request.cpu/memory is used for scheduling and (for CPU) relative weight. limit is a cap: CPU throttle or memory OOMKill. A pod without requests is a noisy neighbor. A tiny request with a huge limit overcommits the node.

                                Heap (-Xmx) must sit under the memory limit with room for metaspace and native. 400m on a free Render plan is a cousin of this idea.

                                In interviews, request versus limit, and why missing requests is rude.
                                """)
                        .qa("CPU throttle looks like what?",
                                "Random latency, longer GC, 'the node is fine'. Check throttling metrics before you tune the JVM.",
                                "What if Xmx is above the memory limit?")
                        .qa("Why set requests if you have limits?",
                                "The scheduler needs requests. Limits without requests still overcommit. Quality of service class depends on both.",
                                "Burstable versus Guaranteed?")
                        .sample("Honest size",
                                """
                                        resources:
                                          requests:
                                            memory: "256Mi"
                                            cpu: "100m"
                                          limits:
                                            memory: "512Mi"
                                        """,
                                "Xmx stays under 512Mi."),
                Concepts.of("Services and DNS",
                                "A Service is a stable name and a selector. Pods come and go. You call skill-studio:8080, not a pod IP.",
                                "A client hardcoded a pod IP and died on the first rollout. Cluster DNS plus the Service name survived.")
                        .depth("""
                                ClusterIP is the default: internal virtual IP, kube-proxy or IPVS to pod endpoints. Only Ready pods (by default) are endpoints. Headless Services return pod IPs for StatefulSets. NodePort and LoadBalancer are how you get in from outside, often behind an Ingress.

                                DNS name is <service>.<namespace>.svc.cluster.local. Short names work in the same namespace. Do not cache pod IPs in your app.

                                In interviews, selector, endpoints, and Ready.
                                """)
                        .qa("Why did the Service have no endpoints?",
                                "Selector matched nothing, or no pod was Ready. Check labels and readiness, not the VIP.",
                                "What is a headless Service?")
                        .qa("Can you call a pod IP from another namespace?",
                                "You can until the pod dies. Call the Service DNS name instead.",
                                "Ingress versus LoadBalancer?")
                        .sample("Stable name",
                                """
                                        apiVersion: v1
                                        kind: Service
                                        spec:
                                          selector:
                                            app: skill-studio
                                          ports:
                                            - port: 80
                                              targetPort: 8080
                                        """,
                                "Pods are endpoints only when Ready."),
                Concepts.of("Horizontal Pod Autoscaler",
                                "HPA adds replicas from CPU, memory, or custom metrics. It cannot fix a single-threaded bottleneck or a missing index.",
                                "CPU-based HPA scaled a CPU-bound report job usefully, and did nothing for a DB-bound API until the index existed.")
                        .depth("""
                                HPA watches metrics and patches the Deployment replica count between min and max. CPU utilization relative to requests is the common signal — so requests must be honest. Custom metrics (queue depth) are better for workers.

                                Scale-down delay exists so you do not flap. Autoscaling pods that cannot run two copies (a singleton locking a row forever) will surprise you.

                                In interviews, metric, min/max, and a case HPA will not save.
                                """)
                        .qa("Why didn't HPA scale?",
                                "No metrics-server, requests unset so utilization is nonsense, or the bottleneck is the database. Check the HPA status.",
                                "When is queue depth a better metric than CPU?")
                        .qa("Can HPA replace a connection pool bulkhead?",
                                "No. More pods times pool size can knock over Postgres. Scale the app and the DB as a system.",
                                "What is flapping?")
                        .sample("CPU HPA",
                                """
                                        spec:
                                          minReplicas: 2
                                          maxReplicas: 8
                                          metrics:
                                            - type: Resource
                                              resource:
                                                name: cpu
                                                target:
                                                  type: Utilization
                                                  averageUtilization: 70
                                        """,
                                "Needs honest CPU requests."),
                Concepts.of("Jobs and CronJobs",
                                "A Job runs to completion. A CronJob creates Jobs on a schedule. Neither is a Deployment. Restarts and concurrencyPolicy are the sharp edges.",
                                "A nightly vacuum-adjacent cleanup used a CronJob with concurrencyPolicy Forbid so a slow run did not overlap.")
                        .depth("""
                                Jobs track completions and backoffLimit. A failing Job will retry until the limit, then sit Failed. CronJobs spawn Jobs; if a run is still going, Forbid skips, Replace kills, Allow overlaps.

                                Do not use a Deployment for a one-shot migration. Do not use a CronJob for a long-lived consumer.

                                In interviews, completions versus parallelism, and concurrencyPolicy.
                                """)
                        .qa("Deployment or Job for a Flyway migrate?",
                                "Init container or a Job, not a replica that keeps restarting migrate. Flyway in the app start is a common Boot choice — still not a CronJob.",
                                "What does backoffLimit do?")
                        .qa("A CronJob overlapped and double-sent mail. Which policy?",
                                "Forbid, and make the work idempotent anyway. Allow is how overlap happens.",
                                "Where do you see the last Job's logs?")
                        .sample("No overlap",
                                """
                                        spec:
                                          schedule: "0 2 * * *"
                                          concurrencyPolicy: Forbid
                                        """,
                                "Slow runs skip the next tick.")
        );
    }
}
