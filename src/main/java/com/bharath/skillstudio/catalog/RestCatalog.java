package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class RestCatalog {

    private RestCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("rest", "REST APIs",
                "REST is resources and verbs. Status codes, idempotency, pagination, and problem bodies are what make a public API survivable.",
                Concepts.of("Resources, not RPC",
                                "Nouns in the path, verbs in the method. /applications/123 plus POST to create, not /doApply. If you cannot GET it later, you built RPC over HTTP.",
                                "An apply queue created an application resource the client could GET after a timeout, instead of a fire-and-forget /applyJob that double-submitted.")
                        .depth("""
                                A resource is a noun you can bookmark. POST creates on a collection, PUT replaces a known id, PATCH patches, DELETE removes, GET reads. /doApply hides the thing you created. If you cannot write a test that GETs it, you do not have a resource.

                                Collection endpoints return 201 with Location on create. The body can be small. RPC-style actions exist (POST /applications/123/cancel) when cancel is not a field you can PATCH honestly — use them sparingly and still return a resource.

                                Controllers named ApplyController that only expose verbs will accrete query parameters forever. Model the application, the job, the resume. In interviews, rewrite a /doX path as resources without sounding religious about HATEOAS.
                                """)
                        .qa("Why not POST /applyJob?",
                                "You lost the application id, you cannot idempotently GET it, and every new action becomes another verb. POST /applications is a collection.",
                                "When is an RPC-style path acceptable?")
                        .qa("What do you return on create?",
                                "201 Created, Location: /applications/{id}, and a body small enough to confirm. 200 with no id is how clients double-submit.",
                                "PUT versus POST for an apply?")
                        .sample("Create an application",
                                """
                                        @PostMapping("/applications")
                                        public ResponseEntity<Void> create(@RequestBody @Valid ApplyRequest req) {
                                            var id = applications.submit(req);
                                            return ResponseEntity.created(URI.create("/applications/" + id)).build();
                                        }
                                        """,
                                "The client can GET the resource after a retry."),
                Concepts.of("Status codes that mean something",
                                "HTTP clients, caches, and on-call already understand 2xx, 4xx, 5xx. Hiding errors in 200 with success:false forces every caller to invent a protocol.",
                                "A search endpoint returned 200 with an error string when the portal timed out. The UI showed an empty list. 504/503 with a problem body made retries and alerts possible.")
                        .depth("""
                                200 is success with a body. 201 created. 204 no body. 400 is the client's document. 401 unauthenticated, 403 authenticated but not allowed — do not mix them. 404 is unknown resource; do not use it to hide existence if that is a security requirement, and do not use it for 'search had zero hits' on a collection GET (that is 200 with []). 409 conflict, 412 precondition, 429 rate limit, 500 your bug, 502/503/504 the dependency.

                                Pick one code and document it. Mapping every exception to 500 trains on-call to ignore 500. Mapping validation to 200 trains clients to parse essays.

                                Idempotent methods have extra meaning: GET, PUT, DELETE retries are safer. POST needs help (next concept). In interviews, give the code and the client action, not a memorized table of 40 statuses.
                                """)
                        .qa("Empty search results: 404 or 200?",
                                "200 with an empty collection. 404 means the search resource itself is missing, not that no jobs matched.",
                                "401 versus 403?")
                        .qa("When is 409 the right status?",
                                "The resource exists and the request conflicts with its current state — duplicate apply, version clash. Not 'we had a NullPointerException'.",
                                "What should a gateway timeout be?")
                        .sample("Collection empty is still OK",
                                """
                                        @GetMapping("/applications")
                                        public List<ApplicationDto> list() {
                                            return applications.list(); // may be empty
                                        }
                                        """,
                                "Zero rows is 200 []."),
                Concepts.of("Idempotency keys",
                                "POST is not idempotent unless you make it so. Clients retry on timeouts. An Idempotency-Key stored with the result turns a second POST into a replay of the first.",
                                "A recruiter double-click sent two identical mails until the apply API keyed on Idempotency-Key plus a body hash.")
                        .depth("""
                                Timeouts happen in the caller after the server succeeded. Without a key, a retry is a second apply. Store the key with the request fingerprint and the response. Same key, same body, same result. Same key, different body, 409. Unknown key, process and remember.

                                Keys need a TTL and a uniqueness constraint in storage. In-flight requests with the same key should wait or 409, not fork. PUT to a client-generated id is another school; many UIs cannot mint that id, so keys fit POST-to-collection.

                                This is not Kafka idempotence. It is an HTTP product rule. In interviews, walk a double-click and a timeout as two stories that share the table.
                                """)
                        .qa("A recruiter double-clicks Send. What happens?",
                                "Same Idempotency-Key, same body, return the first application's id. Different key, two applications — that is a product choice you document.",
                                "Where do you store the key?")
                        .qa("Is PUT enough instead of a key?",
                                "PUT to a client-generated id can work. Many UIs cannot mint that id. Keys fit POST-to-collection. They are complementary.",
                                "What if the first request is still in flight?")
                        .sample("Header in, same result out",
                                """
                                        @PostMapping("/applications")
                                        public ApplicationDto apply(@RequestHeader("Idempotency-Key") String key,
                                                                   @RequestBody ApplyRequest req) {
                                            return applications.submitOnce(key, req);
                                        }
                                        """,
                                "Timeouts stop duplicating mail."),
                Concepts.of("Pagination and ETags",
                                "List endpoints page. Cacheable GET uses ETag. Do not dump ten thousand jobs in one JSON.",
                                "A tracker poll sent the full list every five seconds. ETag plus If-None-Match dropped it to 304 when nothing changed.")
                        .depth("""
                                Offset pagination is simple and breaks when rows shift (a new insert at the top duplicates a row on page 2). Cursor (keyset) pagination using a stable sort (id, or posted_at+id) is the production default for feeds. Return next cursor, not only page numbers.

                                ETag is a fingerprint of the representation. If-None-Match on GET yields 304 with no body. Weak ETags can ignore cosmetic changes. Last-Modified is the older cousin. Do not invent a hash you cannot compute cheaply — version column or a updated_at is enough.

                                Link rel=next is enough HATEOAS for most teams. In interviews, contrast offset versus cursor, and say when 304 is worth it.
                                """)
                        .qa("Why do page numbers lie?",
                                "Inserts and deletes shift offsets. A cursor on a unique sort key does not skip or duplicate when the list grows at the head.",
                                "What do you return at the end of the list?")
                        .qa("When do you send 304?",
                                "GET with If-None-Match matching the current ETag. The client already has the bytes. Lists that poll should support this.",
                                "Weak versus strong ETag?")
                        .sample("Cursor query",
                                """
                                        @GetMapping("/jobs")
                                        public PageDto list(@RequestParam(required = false) String cursor,
                                                            @RequestParam(defaultValue = "20") int size) {
                                            return jobs.page(cursor, size);
                                        }
                                        """,
                                "Response includes next cursor, not only page=3."),
                Concepts.of("Problem details",
                                "application/problem+json beats a random {error: 'oops'}. Status, type, title, and a field path. Spring Boot 3 can emit this from ErrorResponseException.",
                                "An empty jobId on the classroom API tells the UI which field to highlight instead of a stack trace.")
                        .depth("""
                                RFC 9457 (successor to 7807) defines type, title, status, detail, instance. Field errors belong in a well-known extension. Clients branch on type or status, not on English sentences. Do not put stack traces in the body of a public API.

                                Spring's ProblemDetail and ErrorResponseException are the Boot 3 way. Binding failures should name the field. 200 with success:false is a private protocol that caches and HTTP log aggregators will misread.

                                Keep type URIs stable. In interviews, sketch the JSON and say what the UI does with a pointer.
                                """)
                        .qa("Why not return 200 with success:false?",
                                "Caches, logs, and HTTP clients already understand 4xx. Hiding errors in 200 makes every caller write an extra protocol.",
                                "When is 409 the right status?")
                        .qa("What belongs in the problem body?",
                                "type, title, status, detail, and an instance or field pointer. No stack traces on a public API.",
                                "How does Bean Validation map to problems?")
                        .sample("Advice returns a problem",
                                """
                                        @ExceptionHandler(IllegalArgumentException.class)
                                        ResponseEntity<ProblemDetail> bad(IllegalArgumentException e) {
                                            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
                                            problem.setTitle("Bad request");
                                            return ResponseEntity.badRequest().body(problem);
                                        }
                                        """,
                                "The UI reads status and detail."),
                Concepts.of("Validation at the edge",
                                "@Valid on the body, constraints on query params, and a 400 that names fields. Trusting the client is how you store blank job ids.",
                                "A generated studio zip rejected a blank skill list with 400 before StudioProjectWriter threw a 500.")
                        .depth("""
                                Validation is part of the HTTP contract. @NotBlank, @Email, @Size, custom validators for business formats. Validate path variables too (@Min on a page size). Default page size in this app is 4 — a client sending size=10000 should be capped or rejected.

                                @RequestBody needs @Valid to trigger. Forgetting it is a common bug: constraints sit on the record, the method still accepts junk. Tests should post invalid JSON and expect 400, not 500.

                                Sanitize after validate. Validation is not HTML encoding. In interviews, name the annotation that actually triggers, and the exception.
                                """)
                        .qa("Constraints on the record never fire. Why?",
                                "The controller parameter is missing @Valid or @Validated. Annotations are metadata until a validator runs.",
                                "How do you test it?")
                        .qa("Query param size=0 — validate or default?",
                                "This classroom defaults size to 4 when missing or less than 1. A public API might 400 instead. Pick one and document it.",
                                "What about size=100000?")
                        .sample("Trigger the validator",
                                """
                                        @PostMapping("/applications")
                                        public ApplicationDto apply(@Valid @RequestBody ApplyRequest req) {
                                            return applications.submit(req);
                                        }
                                        """,
                                "Without @Valid the annotations are comments."),
                Concepts.of("Caching headers",
                                "GET that can be reused needs Cache-Control. Private data is private. A shared cache that stores an Authorization-scoped body is a leak.",
                                "A skills catalog GET was public and cacheable for a minute. A user-specific tracker GET was Cache-Control: private, no-store.")
                        .depth("""
                                Cache-Control: public, max-age is for representations that do not depend on the user. private is for a browser cache only. no-store is for tokens and PII. Vary tells caches which request headers change the body (Accept, Authorization — be careful).

                                ETag plus Cache-Control work together: revalidate with If-None-Match. Do not set max-age=31536000 on an API that ships hourly data. CDNs will honor what you send; lying is an incident.

                                POST is not cached. GET must be safe. In interviews, pick headers for a public catalog versus a private tracker.
                                """)
                        .qa("Can you cache a GET that used Authorization?",
                                "Not on a shared cache unless you know what you are doing. private or no-store. Vary: Authorization is a footgun.",
                                "When is max-age appropriate on an API?")
                        .qa("What does no-store protect?",
                                "Intermediary and browser storage of a sensitive representation. Use it for tokens and personal trackers.",
                                "How does ETag interact with max-age?")
                        .sample("Public catalog",
                                """
                                        @GetMapping("/api/skills.json")
                                        public ResponseEntity<?> skills() {
                                            return ResponseEntity.ok()
                                                    .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                                                    .body(body);
                                        }
                                        """,
                                "User-specific GETs should not copy this."),
                Concepts.of("Version only when you break",
                                "URI versioning is a last resort. Additive fields are cheaper. Deprecate with a header and a sunset date.",
                                "Adding a match reason did not force /v2/search. A later rename of jobId to listingId did, and the old path stayed until clients moved.")
                        .depth("""
                                Additive JSON fields are backward compatible if clients ignore unknowns. Removing or renaming is a break. Changing the meaning of a field without renaming is the worst break — silent corruption.

                                /v1/ in the path is honest but expensive. Header versioning (Accept: application/vnd.foo.v2+json) is flexible and harder to debug. Most teams version the path when they truly break, and document a sunset.

                                Do not version because you added a column. In interviews, give an additive change and a breaking change, and say how long v1 lives.
                                """)
                        .qa("Does a new optional field require /v2?",
                                "No. Clients should ignore unknowns. /v2 is for breaks: removed fields, changed meaning, new required fields without defaults.",
                                "How do you retire v1?")
                        .qa("Path versus Accept versioning?",
                                "Path is greppable and cache-friendly. Accept is flexible. Pick one per product; mixing them is how docs rot.",
                                "What is a silent break?")
                        .sample("Additive DTO",
                                """
                                        public record JobDto(String id, String title, String company, String matchReason) {}
                                        """,
                                "Old clients ignore matchReason. No /v2.")
        );
    }
}
