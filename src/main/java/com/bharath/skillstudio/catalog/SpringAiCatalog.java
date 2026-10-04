package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.SkillCurriculum;
import com.bharath.skillstudio.learn.SkillCurriculum.Outline;

public final class SpringAiCatalog {

    private SpringAiCatalog() {
    }

    public static Outline outline() {
        return SkillCurriculum.outline("spring-ai", "Spring AI",
                "Spring AI is one ChatClient and a handful of advisors. Everything else is a prompt, a Java bean, or a tool. This classroom does not call a model — it teaches the shape.",
                Concepts.of("ChatClient is the door",
                                "You do not call the model HTTP API by hand. One client, system plus user, then call or stream. Swap the model in config.",
                                "Scoring, cover letters, and a lab share one ChatClient bean. A second client per button is how prompts drift.")
                        .depth("""
                                ChatClient is the portable call site. System message sets role. User message is the task. call() waits for a ChatResponse. stream() yields tokens. The model (Gemini, OpenAI, Ollama) is configuration on a ChatModel bean, not a new client class per feature.

                                Building HTTP yourself re-implements retries, streaming, tool calling, and structured output. Spring AI already did. One ChatClient in config, many prompts in services.

                                In interviews, draw prompt → ChatClient → ChatModel, and say where the key lives (env, not code).
                                """)
                        .qa("Why not RestTemplate to the model HTTP API?",
                                "You re-implement retries, streaming, tools, and JSON schema. ChatClient is the portable call site. The provider is configuration.",
                                "Where is the client built?")
                        .qa("call versus stream?",
                                "call when you persist a finished document. stream when the UI should type. Do not persist a half-written stream.",
                                "How does a servlet app handle a Flux?")
                        .sample("One call site",
                                """
                                        String letter = chatClient.prompt()
                                                .system("You are a concise cover-letter writer.")
                                                .user(userMessage)
                                                .call()
                                                .content();
                                        """,
                                "The model is not in this method."),
                Concepts.of("Structured output",
                                "entity() asks the model for JSON that matches a class. That is how a score becomes a column instead of a paragraph.",
                                "Search stores a numeric score. If the model returns garbage, a heuristic fallback still has to give you a number.")
                        .depth("""
                                Models write prose. Your database wants fields. entity(Class) sends a schema and binds JSON onto a Java type. Mutable classes with setters bind reliably; records have historically been picky depending on mapper config — prove it before you standardize.

                                Fail closed: catch bind failures, log the raw completion (carefully — resumes are PII), fall back. Never regex the prose after you already asked for a bean.

                                In interviews, the bean fields, the fallback, and why a record might stay empty.
                                """)
                        .qa("Why did my record stay empty?",
                                "Jackson typically needs setters or a well-annotated constructor. Use a mutable class for entity() until you have proven records on your mapper.",
                                "What do you log when bind fails?")
                        .qa("How do you keep Search up when the model returns markdown fences?",
                                "Fail closed to a heuristic. Read the logger advisor for the raw completion. Do not show a stack trace on a user page.",
                                "Should you repair the JSON yourself?")
                        .sample("Score as a bean",
                                """
                                        MatchResult result = chatClient.prompt()
                                                .user(prompt)
                                                .call()
                                                .entity(MatchResult.class);
                                        """,
                                "score, reasons, gaps — columns, not an essay."),
                Concepts.of("Advisors",
                                "Advisors intercept. Logger records. Memory remembers. They are not a second client.",
                                "When entity() fails, you read the logger, not the stack trace, to see the raw completion.")
                        .depth("""
                                An advisor wraps a ChatClient call the way a filter wraps HTTP. SimpleLoggerAdvisor prints prompt and completion. Memory advisors prepend history. Order matters. You attach defaults on the builder, or per-request advisors for tools and memory.

                                A second ChatClient to get logging is how log levels and prompts drift. One client, advisors for cross-cutting.

                                In interviews, interceptor analogy, and which advisors are default versus per-call.
                                """)
                        .qa("Is an advisor a second ChatClient?",
                                "No. One client, a chain of advisors. A second client is how prompts and log levels drift per button.",
                                "Where would you attach a logger?")
                        .qa("Can logging advisors leak PII?",
                                "Yes. Prompts contain resumes. Tighten before a company deploy. A laptop flight recorder is not a cluster default.",
                                "What is advisor order?")
                        .sample("Logger on the client",
                                """
                                        ChatClient.builder(model)
                                                .defaultAdvisors(new SimpleLoggerAdvisor())
                                                .build();
                                        """,
                                "One client. The advisor is the flight recorder."),
                Concepts.of("Tool calling",
                                "@Tool methods let the model ask the app for facts instead of inventing tracker counts.",
                                "A career assistant that quotes actual Java jobs must call a tool, or it will hallucinate a company.")
                        .depth("""
                                The model receives a function schema from your @Tool methods. It may request a call. You run the method. You return a string. The model then speaks. Tools are not magic access to JPA — they are the methods you exposed.

                                Keep tools small, typed, and side-effect-light until you trust the loop. A tool named sendMail will send mail. Descriptions are Javadoc for the model; vague blurbs get vague calls. Do not hang write tools on a scoring prompt.

                                In interviews, the loop, read-only default, and why pasting the database into the prompt is worse.
                                """)
                        .qa("Why not paste the tracker into the prompt?",
                                "The window is finite and stale. A tool fetches the current row. The model still has to choose to call it — your system prompt should say when.",
                                "How do you stop a tool from doing damage?")
                        .qa("What does the model receive from a tool?",
                                "The return value as text or JSON. If you return a novel, you wasted the window. Return the three numbers it asked for.",
                                "Should scoring prompts get tools?")
                        .sample("Facts, not guesses",
                                """
                                        @Tool(description = "Count applications by status")
                                        public String trackerCounts() {
                                            return tracker.summarize();
                                        }
                                        """,
                                "The model asks. The app answers."),
                Concepts.of("Retrieve then generate",
                                "RAG is not a different client. You pick evidence, then you ask the model to speak only from those chunks.",
                                "A cover letter that mentions Kafka because the posting asked for Kafka, not because the resume dumped every acronym.")
                        .depth("""
                                Retrieve is search: chunks, overlap scores, or a vector store. Generate is ChatClient with a system line that forbids leaving the evidence. A giant prompt is not RAG. Vector stores upgrade retrieve; they do not replace the generate step.

                                Thin RAG output is often thin retrieve. Fix chunk size, overlap, and the query before you blame the model. Log which chunks you sent (PII-aware).

                                In interviews, two verbs, one client, and when to add embeddings.
                                """)
                        .qa("Is RAG a different ChatClient?",
                                "No. Same client. Different user message, and a system line that forbids going outside the chunks. If you skipped retrieve, you just talked a lot.",
                                "When do you add a vector store?")
                        .qa("Why can RAG sound thinner than dumping the resume?",
                                "The chunks missed the story. Fix retrieve before you blame the model. A thin letter is often a thin evidence set.",
                                "What do you log to debug a bad RAG letter?")
                        .sample("Evidence then the ask",
                                """
                                        var chunks = selector.topChunks(resume, job);
                                        String letter = chatClient.prompt()
                                                .system("Use only the resume chunks. Do not invent jobs.")
                                                .user(job + "\\n\\n" + chunks)
                                                .call()
                                                .content();
                                        """,
                                "Retrieve is the selector. Generate is ChatClient."),
                Concepts.of("Prompt templates",
                                "Prompts are product copy. They do not belong in an eighty-line Java string. PromptTemplate keeps wording in a file with placeholders.",
                                "Cover-letter wording in a .st file meant Apply and a stream button could not drift.")
                        .depth("""
                                PromptTemplate fills placeholders. The rendered string is the user message. Change tone without recompiling the gateway. Null placeholders explode; send empty strings for missing fields.

                                Do not duplicate the template in Java 'just for streaming'. call and stream share the file. Version the file in git like any copy.

                                In interviews, file versus string, and a null placeholder pitfall.
                                """)
                        .qa("Why not a Java text block for the letter?",
                                "Copy will change more often than code. A file is reviewable by non-Java people. Streaming must not fork a second wording.",
                                "What happens on a missing placeholder?")
                        .qa("Who owns the prompt?",
                                "The product. Treat it like UI copy: review, version, do not sneak a tone change in a 200-line class.",
                                "System versus user message?")
                        .sample("Template file",
                                """
                                        PromptTemplate template = new PromptTemplate("classpath:prompts/cover-letter.st");
                                        String user = template.render(Map.of("title", title, "skills", skills));
                                        """,
                                "Wording lives in the file."),
                Concepts.of("Streaming tokens",
                                "call() is a spinner. stream() is a typewriter. Same prompt, different return: a Flux of strings.",
                                "A servlet app subscribed on a worker thread and copied chunks onto an SseEmitter so Tomcat was not blocked on the Flux.")
                        .depth("""
                                ChatClient.stream().content() is reactive. A Boot MVC app is not WebFlux by default. You subscribe on a bounded elastic (or virtual) thread and push SSE to the browser. Persist with call(); demo with stream().

                                Cancellation: if the browser disconnects, cancel the subscription so you do not pay for tokens nobody sees. Do not concatenate a stream into a DB row until complete.

                                In interviews, servlet versus Flux, and SSE.
                                """)
                        .qa("Can you return Flux from a @RestController on Tomcat?",
                                "You can with extra machinery; the default is still a servlet thread. Prefer SseEmitter plus a worker for MVC apps.",
                                "When do you persist?")
                        .qa("What if the client hits Stop?",
                                "Cancel the subscription. Otherwise the model keeps billing while nobody listens.",
                                "SSE versus WebSocket?")
                        .sample("Stream content",
                                """
                                        Flux<String> tokens = chatClient.prompt()
                                                .user(userMessage)
                                                .stream()
                                                .content();
                                        """,
                                "Subscribe off the request thread in MVC."),
                Concepts.of("Chat memory",
                                "A model is goldfish unless you give it a notebook. Memory stores recent messages under a conversation id.",
                                "An interview coach could say 'you already answered Kafka' because MessageWindowChatMemory held the last turns. Scoring did not share that id.")
                        .depth("""
                                MessageWindowChatMemory is a sliding transcript, not a vector database. The advisor reads and writes around each call. Scope it: one conversation id for the coach, none for scoring, or your cover letter quotes the mock interview.

                                Cap the window. Unbounded history is a token bill and a privacy dump. Clear is a product button.

                                In interviews, this is not RAG, and scoping is the punchline.
                                """)
                        .qa("Is memory RAG?",
                                "No. Memory is the last N messages. RAG is retrieve-from-corpus then generate. Do not mix the words.",
                                "What happens if you forget the conversation id?")
                        .qa("Why not put memory on every ChatClient call?",
                                "Scoring JSON must not include yesterday's coaching. Scope advisors per use case.",
                                "How do you cap the window?")
                        .sample("Windowed memory",
                                """
                                        MessageWindowChatMemory.builder()
                                                .maxMessages(16)
                                                .build();
                                        """,
                                "Coach only. Not on score."),
                Concepts.of("Fail closed",
                                "The model will lie, time out, or return markdown fences. The product still has to answer. Heuristics and defaults are senior engineering, not a cop-out.",
                                "This Skill Studio generates unknown skills with a generic huddle and no LLM. Job scoring in a sister app falls back to a heuristic gateway when the key is missing.")
                        .depth("""
                                Timeouts, 429 quota, empty entity, unsafe content — all are expected. Circuit the client, pause on quota, show a message, keep the rest of the app up. Never block a core user journey on a model.

                                Feature flags beat hope. If the key is absent, disable the buttons and say why. Do not catch Exception and return 'success'.

                                In interviews, name three failure modes and the user-visible fallback.
                                """)
                        .qa("The API key is missing. What should happen?",
                                "The app boots. AI features disable. Non-AI paths still work. Do not crash the context on a missing optional key.",
                                "What do you do with HTTP 429?")
                        .qa("Is a heuristic scorer cheating?",
                                "It is a bulkhead. Staff engineers ship a number when the model is drunk. You can still show that the AI path failed.",
                                "Should you retry forever?")
                        .sample("Optional client",
                                """
                                        if (apiKey == null || apiKey.isBlank()) {
                                            return heuristic.score(job, resume);
                                        }
                                        return chat.score(job, resume);
                                        """,
                                "Search still returns.")
        );
    }
}
