package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class AwsSdkCatalog {

    private AwsSdkCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("aws-sdk", "AWS SDK for Java",
                "The v2 SDK is a client per service, with credentials from the environment, not from a pasted key in code.",
                Concepts.of("Default credentials chain",
                                "Env vars, then a profile, then the instance or container role. Never hardcode. Never commit.",
                                "The same binary works on a laptop with a profile and in ECS with a task role. A key in application.yml would have leaked in the image.")
                        .depth("""
                                DefaultCredentialsProvider walks the chain: system properties, env (AWS_ACCESS_KEY_ID), web identity (IRSA), instance profile, etc. S3Client.create() uses it. Static keys in code are a finding. Long-lived IAM users for apps are a finding; prefer roles.

                                Profiles (~/.aws/credentials) are for laptops. Production is a role. STS assume-role is how CI gets short credentials.

                                In interviews, the chain order you actually rely on, and why the jar has no key.
                                """)
                        .qa("How does ECS supply credentials?",
                                "A task role, picked up by the chain. You do not paste keys into the task definition's app config.",
                                "What is IRSA?")
                        .qa("Can you commit ~/.aws/credentials?",
                                "No. And you do not copy it into the image. Local profile stays local.",
                                "Why short-lived credentials?")
                        .sample("Client from the chain",
                                """
                                        S3Client s3 = S3Client.create();
                                        """,
                                "Credentials are not in this line."),
                Concepts.of("Clients are beans",
                                "Build the client once. Share it. It is thread-safe. Creating one per request is how you leak connections.",
                                "A resume upload path used one S3 client for the whole JVM. Per-request create() showed up as TIME_WAIT exhaustion.")
                        .depth("""
                                SDK v2 clients are thread-safe. They own HTTP connections. Close them when the Spring context closes (@PreDestroy or AutoCloseable bean). Region is part of the client.

                                Do not hide create() inside a loop. Do not store credentials on the client beyond what the provider already does.

                                In interviews, bean lifecycle and thread safety.
                                """)
                        .qa("Is S3Client thread-safe?",
                                "Yes. One bean. Close on shutdown.",
                                "What leaks if you create per request?")
                        .qa("Where does region live?",
                                "On the client builder, or AWS_REGION. Mixing regions on one client is not a thing — build another if you must.",
                                "When do you override the endpoint?")
                        .sample("Spring bean",
                                """
                                        @Bean
                                        S3Client s3Client() {
                                            return S3Client.builder()
                                                    .region(Region.AP_SOUTH_1)
                                                    .build();
                                        }
                                        """,
                                "Destroy method closes."),
                Concepts.of("Retries and timeouts",
                                "The SDK retries. Your timeout must be larger than the retry budget or you pile on.",
                                "A flaky S3 put of a resume PDF retried without the user double-clicking upload. The HTTP client timeout sat above the retry window.")
                        .depth("""
                                RetryMode (STANDARD, ADAPTIVE) and max attempts live on ClientOverrideConfiguration. Timeouts: attempt timeout versus call timeout. Call timeout covers the whole retry sequence. If the caller's HTTP timeout is shorter, they retry while you still retry — thundering herd.

                                Not all errors are retryable. 403 is not. 429 and 5xx often are. Idempotent puts to a chosen key are safer to retry than a non-idempotent side effect.

                                In interviews, call versus attempt timeout, and a 403 you must not retry.
                                """)
                        .qa("Why is my client still running after the user gave up?",
                                "Call timeout longer than the servlet timeout, or retries continuing. Align budgets.",
                                "Which errors should not retry?")
                        .qa("Is retry twice always safer?",
                                "On a PUT to a fixed object key, yes. On a 'create charge' API, no. Know the verb.",
                                "What is adaptive retry?")
                        .sample("Override timeouts",
                                """
                                        S3Client.builder()
                                                .overrideConfiguration(c -> c
                                                        .apiCallTimeout(Duration.ofSeconds(20))
                                                        .apiCallAttemptTimeout(Duration.ofSeconds(8)))
                                                .build();
                                        """,
                                "Call > attempt × retries."),
                Concepts.of("Pagination",
                                "List APIs are paged. The SDK's paginator exists so you do not invent a while-token loop with a bug.",
                                "A bucket of stored cover letters listed completely via listObjectsV2Paginator before the app said 'nothing to apply'.")
                        .depth("""
                                S3 ListObjectsV2 returns a token. Missing a loop drops objects. Paginators (listObjectsV2Paginator) flatten pages. Bound the max keys you will process in one job so you cannot hang forever.

                                Other services (SQS receive, DynamoDB scan) have the same shape. Never assume one page.

                                In interviews, continuation token and paginator.
                                """)
                        .qa("What happens if you ignore NextContinuationToken?",
                                "You see the first page and believe that is the bucket. Jobs go missing.",
                                "How do you cap a list job?")
                        .qa("Scan versus query in DynamoDB?",
                                "Scan reads the table. Query uses a key. Pagination still applies to both. Scan is the expensive default you should not start with.",
                                "Does a paginator load all into memory?")
                        .sample("Paginator",
                                """
                                        s3.listObjectsV2Paginator(r -> r.bucket("letters"))
                                                .contents()
                                                .forEach(obj -> log.info(obj.key()));
                                        """,
                                "Still cap in a real job."),
                Concepts.of("IAM least privilege",
                                "The role should PutObject to one prefix, not * on *.",
                                "A leaked task role should not be able to read every bucket in the account. resumes/* write-only was the ticket.")
                        .depth("""
                                Identity (who) versus resource policy (on the bucket). The task role's policy is the first gate. s3:PutObject on arn:aws:s3:::bucket/resumes/* is a start. ListBucket needs a prefix condition or you leak key names.

                                Access keys in an IAM user with AdministratorAccess is how intern laptops become incidents. Prefer roles. Review with Access Analyzer.

                                In interviews, an ARN and an action list, not 'we use IAM'.
                                """)
                        .qa("PutObject on * is wrong why?",
                                "A bug or SSRF can write anywhere, or read if you also granted Get. Prefix-scope the ARN.",
                                "Does the bucket policy still matter?")
                        .qa("ListBucket without a prefix condition?",
                                "The caller can enumerate keys. Often more sensitive than you think.",
                                "IAM user versus role for an app?")
                        .sample("Tight action",
                                """
                                        {
                                          "Action": "s3:PutObject",
                                          "Resource": "arn:aws:s3:::acme-resumes/resumes/*"
                                        }
                                        """,
                                "JSON policy sketch. Screen only."),
                Concepts.of("S3 as object store",
                                "S3 is bytes with a key, not a filesystem. Eventual listing, strong read-after-write for new puts of new keys, and no real rename.",
                                "Resume PDFs went to s3://bucket/resumes/{user}/{uuid}.pdf. The DB stored the key, not a local path that died with the pod.")
                        .depth("""
                                Keys are strings, often path-shaped. There are no real directories. Copy plus delete is rename. SSE-S3 or SSE-KMS for encryption. Pre-signed URLs for browser uploads without passing the bucket through the app.

                                Never open a bucket for public write. Website hosting is a different pattern than private resumes.

                                In interviews, key design, presign, and why the DB stores the key.
                                """)
                        .qa("Why not save uploads on the pod disk?",
                                "Pods are cattle. S3 (or a PVC) outlives them. The database points at the object key.",
                                "What is a pre-signed URL?")
                        .qa("Can you chmod an S3 object like a file?",
                                "No. ACLs are legacy; bucket policies and IAM are the model. Public ACLs are how leaks happen.",
                                "Read-after-write?")
                        .sample("Put a resume",
                                """
                                        s3.putObject(r -> r.bucket("acme-resumes").key(key),
                                                RequestBody.fromFile(pdf));
                                        """,
                                "DB saves bucket+key."),
                Concepts.of("SQS versus SNS",
                                "SQS is a queue (competing consumers). SNS is fanout (topics to many subscribers). They compose: SNS to several SQS queues.",
                                "ApplicationSubmitted went to SNS; mail and tracker each had an SQS subscription so they scaled independently.")
                        .depth("""
                                SQS: at-least-once, visibility timeout as a lock, DLQ on max receive. Idempotency required. FIFO queues have ordering per group id and less throughput.

                                SNS: pub/sub. Email, SQS, HTTP, Lambda subscribers. A missed HTTP subscriber is a retry by SNS, not a durable backlog unless the subscriber is SQS.

                                In interviews, competing versus fanout, visibility timeout, DLQ.
                                """)
                        .qa("Why a visibility timeout?",
                                "The message hides while you work. If you crash, it reappears. Too short and two workers overlap; too long and poison is slow.",
                                "When FIFO?")
                        .qa("SNS directly to HTTP versus SNS to SQS?",
                                "HTTP can miss when you are down. SQS keeps the message. Prefer SNS→SQS for work you must not lose.",
                                "How does a DLQ get populated?")
                        .sample("Receive and delete",
                                """
                                        var msgs = sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(5));
                                        // work
                                        sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(handle));
                                        """,
                                "Delete after success. Idempotent work."),
                Concepts.of("Region and endpoints",
                                "Region is a hard boundary. A client in ap-south-1 does not see buckets created in us-east-1. LocalStack and VPC endpoints override the URL on purpose.",
                                "A laptop pointed at LocalStack with an endpoint override. Prod omitted the override and used the real regional endpoint.")
                        .depth("""
                                Pick the region close to users and data-residency rules. Cross-region is latency and cost. Dual-run is a product, not a client flag you forget.

                                endpointOverride is for testing and PrivateLink. Leaving a staging endpoint in prod is an outage. AWS_ENDPOINT_URL in newer SDKs is the env form — keep it out of prod.

                                In interviews, region as data plane, and when to override endpoint.
                                """)
                        .qa("Bucket not found, but the console shows it?",
                                "Wrong region on the client, or wrong account. Console is us-east-1 by default in some views — check the region switcher.",
                                "What is a VPC endpoint?")
                        .qa("Should prod set endpointOverride?",
                                "No, unless you use PrivateLink and mean it. Staging LocalStack overrides stay in a profile.",
                                "Multi-region active-active?")
                        .sample("Explicit region",
                                """
                                        S3Client.builder().region(Region.of("ap-south-1")).build();
                                        """,
                                "No endpoint override in prod.")
        );
    }
}
