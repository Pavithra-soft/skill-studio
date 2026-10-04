package com.bharath.skillstudio.ai;

import com.bharath.skillstudio.learn.CodeSample;
import com.bharath.skillstudio.learn.CoreConcept;
import com.bharath.skillstudio.learn.InterviewCard;
import com.bharath.skillstudio.learn.LessonCopy;
import com.bharath.skillstudio.learn.LibrarySearch;
import com.bharath.skillstudio.learn.LibrarySearch.Hit;
import com.bharath.skillstudio.learn.SkillLesson;
import com.bharath.skillstudio.learn.SpokenScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TutorService {

    private static final Logger log = LoggerFactory.getLogger(TutorService.class);
    private static final String SYSTEM = """
            You are a staff-level Java / Spring tutor.
            Answer the user's question directly. Do not repeat the question.
            Structure: one-line definition, 4-6 bullets, one watch-out, one takeaway.
            Use only the evidence. Do not invent APIs, class names, or versions.
            Never tell the user to run shell commands. Voice is spoken sentences, never code.
            """;

    private final LlmSupport llmSupport;
    private final Map<String, List<String>> history = new ConcurrentHashMap<>();
    private final Map<String, PendingQuiz> quizzes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> quizCursor = new ConcurrentHashMap<>();

    public TutorService(LlmSupport llmSupport) {
        this.llmSupport = llmSupport == null ? LlmSupport.disabled() : llmSupport;
    }

    public boolean llmEnabled() {
        return llmSupport.enabled();
    }

    public String provider() {
        return llmSupport.providerName();
    }

    public CoreConcept enrich(SkillLesson lesson, CoreConcept focus) {
        CoreConcept base = LessonCopy.polish(focus);
        if (!llmSupport.enabled()) {
            base.setGenerated(true);
            return base;
        }
        try {
            List<Hit> hits = LibrarySearch.search(lesson, focus.getTitle() + " " + focus.getWhy(), focus.getSlug(), 4);
            GeneratedConcept generated = llmSupport.chatClient().prompt()
                    .system(SYSTEM)
                    .user(u -> u.text("""
                                    Skill: {skill}
                                    Concept: {title}
                                    Evidence:
                                    {evidence}

                                    Rewrite this concept as a clean tutor card for a senior interview.
                                    Fill points (5-8 short bullets), trap, takeaway, why (one or two sentences),
                                    depth (three short paragraphs max), two interview Q&A, one small Java sample if the evidence has an API you are sure of.
                                    """)
                            .param("skill", lesson.getName())
                            .param("title", focus.getTitle())
                            .param("evidence", evidence(hits, focus)))
                    .call()
                    .entity(GeneratedConcept.class);
            if (generated == null || generated.getPoints() == null || generated.getPoints().isEmpty()) {
                base.setGenerated(true);
                return base;
            }
            return merge(base, generated);
        } catch (Exception e) {
            log.warn("Concept generate for {} / {} failed: {}", lesson.getKey(), focus.getTitle(), e.getMessage());
            base.setGenerated(true);
            return base;
        }
    }

    public ChatReply chat(SkillLesson lesson, String conceptSlug, String message, String conversationId, boolean quiz) {
        String id = conversationId == null || conversationId.isBlank() ? "studio" : conversationId;
        if (quiz) {
            return startQuiz(lesson, conceptSlug, id);
        }
        String question = message == null ? "" : message.trim();
        if (question.isBlank()) {
            return new ChatReply("Ask a question about this skill, or hit Quiz me.", false, "catalog", List.of());
        }
        PendingQuiz pending = quizzes.remove(id);
        if (pending != null) {
            return gradeQuiz(pending, question);
        }
        List<Hit> hits = LibrarySearch.searchOrWiden(lesson, question, conceptSlug, 3);
        List<String> sources = hits.stream().map(Hit::title).distinct().toList();
        if (llmSupport.enabled()) {
            try {
                List<String> prior = history.computeIfAbsent(id, key -> new ArrayList<>());
                String draft = TutorAnswer.compose(question, hits);
                SkillLesson chatLesson = lesson;
                String answer = llmSupport.chatClient().prompt()
                        .system(SYSTEM + " Prefer the catalog draft. Keep the spoken answer under 10 sentences.")
                        .user(u -> u.text("""
                                        Skill: {skill}
                                        Focus concept: {focus}
                                        Question: {question}
                                        Catalog draft:
                                        {draft}
                                        Recent turns:
                                        {history}
                                        Evidence:
                                        {evidence}
                                        """)
                                .param("skill", chatLesson.getName())
                                .param("focus", conceptSlug == null ? "" : conceptSlug)
                                .param("question", question)
                                .param("draft", draft)
                                .param("history", String.join("\n", prior.subList(Math.max(0, prior.size() - 6), prior.size())))
                                .param("evidence", evidence(hits, null)))
                        .call()
                        .content();
                if (answer != null && !answer.isBlank()) {
                    prior.add("User: " + question);
                    prior.add("Tutor: " + answer);
                    ChatReply reply = new ChatReply(answer.trim(), true, llmSupport.providerName(), sources);
                    reply.setVoice(SpokenScript.sanitize(TutorAnswer.voiceOf(answer)));
                    return reply;
                }
            } catch (Exception e) {
                log.warn("Tutor chat failed: {}", e.getMessage());
            }
        }
        return catalogReply(question, hits, sources);
    }

    public static String catalogAnswer(String question, List<Hit> hits) {
        return TutorAnswer.compose(question, hits);
    }

    private ChatReply catalogReply(String question, List<Hit> hits, List<String> sources) {
        String answer = TutorAnswer.compose(question, hits);
        ChatReply reply = new ChatReply(answer, false, "catalog", sources);
        reply.setVoice(SpokenScript.sanitize(TutorAnswer.voiceOf(answer)));
        return reply;
    }

    private ChatReply startQuiz(SkillLesson lesson, String conceptSlug, String conversationId) {
        List<InterviewCard> cards = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        collectCards(lesson, conceptSlug, cards, titles);
        if (cards.isEmpty()) {
            collectCards(lesson, "", cards, titles);
        }
        if (cards.isEmpty()) {
            return new ChatReply("No interview card on this concept yet. Ask me a question instead.", false, "catalog",
                    List.of());
        }
        int index = quizCursor.computeIfAbsent(conversationId, key -> new AtomicInteger()).getAndIncrement();
        int slot = Math.floorMod(index, cards.size());
        InterviewCard card = cards.get(slot);
        String title = titles.get(slot);
        quizzes.put(conversationId, new PendingQuiz(card.getQuestion(), card.getAnswer(), card.getFollowUp(), title));
        String text = "Quiz. " + card.getQuestion()
                + " Answer in your own words, then I will score it against the catalog.";
        ChatReply reply = new ChatReply(text, false, llmEnabled() ? provider() : "catalog", List.of(title));
        reply.setVoice(SpokenScript.sanitize(text));
        return reply;
    }

    private ChatReply gradeQuiz(PendingQuiz pending, String userAnswer) {
        String graded = TutorAnswer.grade(userAnswer, pending.answer(), pending.followUp());
        ChatReply reply = new ChatReply(graded, false, "catalog", List.of(pending.source()));
        reply.setVoice(SpokenScript.sanitize(TutorAnswer.voiceOf(graded)));
        return reply;
    }

    private static void collectCards(SkillLesson lesson, String conceptSlug, List<InterviewCard> cards, List<String> titles) {
        if (lesson.getConcepts() == null) {
            return;
        }
        String focus = conceptSlug == null ? "" : conceptSlug.trim();
        for (CoreConcept concept : lesson.getConcepts()) {
            if (concept == null || concept.getInterviews() == null) {
                continue;
            }
            if (!focus.isBlank() && !focus.equals(concept.getSlug())) {
                continue;
            }
            for (InterviewCard card : concept.getInterviews()) {
                if (card != null && card.getQuestion() != null && !card.getQuestion().isBlank()
                        && card.getAnswer() != null && !card.getAnswer().isBlank()) {
                    cards.add(card);
                    titles.add(concept.getTitle());
                }
            }
        }
    }

    private static CoreConcept merge(CoreConcept base, GeneratedConcept generated) {
        if (generated.getTitle() != null && !generated.getTitle().isBlank()) {
            base.setTitle(generated.getTitle());
        }
        if (generated.getWhy() != null && !generated.getWhy().isBlank()) {
            base.setWhy(generated.getWhy());
        }
        if (generated.getDepth() != null && !generated.getDepth().isBlank()) {
            base.setDepth(generated.getDepth());
        }
        if (generated.getTrap() != null && !generated.getTrap().isBlank()) {
            base.setTrap(generated.getTrap());
        }
        if (generated.getTakeaway() != null && !generated.getTakeaway().isBlank()) {
            base.setTakeaway(generated.getTakeaway());
        }
        if (generated.getUseCase() != null && !generated.getUseCase().isBlank()) {
            base.setUseCase(generated.getUseCase());
        }
        if (generated.getPoints() != null && !generated.getPoints().isEmpty()) {
            base.setPoints(generated.getPoints());
        }
        if (generated.getInterviews() != null && !generated.getInterviews().isEmpty()) {
            base.setInterviews(generated.getInterviews());
        }
        if (generated.getSamples() != null && !generated.getSamples().isEmpty()) {
            base.setSamples(generated.getSamples());
        } else if (generated.getExample() != null && !generated.getExample().isBlank() && base.getSamples().isEmpty()) {
            base.getSamples().add(new CodeSample("Example", "java", generated.getExample(), "Screen only."));
        }
        base.setGenerated(true);
        return LessonCopy.polish(base);
    }

    private static String evidence(List<Hit> hits, CoreConcept focus) {
        StringBuilder out = new StringBuilder();
        if (focus != null) {
            out.append("Focus title: ").append(focus.getTitle()).append('\n');
            out.append(focus.getWhy()).append('\n');
            if (focus.getDepth() != null) {
                String depth = focus.getDepth();
                out.append(depth, 0, Math.min(depth.length(), 1800)).append('\n');
            }
        }
        for (Hit hit : hits) {
            out.append("### ").append(hit.title()).append('\n');
            out.append(hit.body(), 0, Math.min(hit.body().length(), 1200)).append('\n');
        }
        return out.toString();
    }

    record PendingQuiz(String question, String answer, String followUp, String source) {
    }
}
