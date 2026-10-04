package com.bharath.skillstudio.learn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Retrieves the most relevant authored lesson chunks for a question.
 * Distinctive terms and title phrases beat generic words like "use" or "java".
 */
public final class LibrarySearch {

    public record Hit(String skillKey, String skillName, String conceptSlug, String title, String body, int score,
                      String kind) {
        public Hit(String skillKey, String skillName, String conceptSlug, String title, String body, int score) {
            this(skillKey, skillName, conceptSlug, title, body, score, "concept");
        }
    }

    public record Query(List<String> terms, String phrase, boolean wantsPattern, boolean wantsStandard) {
        public static Query parse(String question) {
            List<String> terms = expand(tokens(question));
            String phrase = phraseOf(terms);
            String lower = question == null ? "" : question.toLowerCase(Locale.ROOT);
            boolean pattern = lower.contains("pattern") || lower.contains("solid") || lower.contains("principle");
            boolean standard = lower.contains("standard") || lower.contains("convention") || lower.contains("style");
            return new Query(terms, phrase, pattern, standard);
        }
    }

    private LibrarySearch() {
    }

    public static List<Hit> search(SkillLesson lesson, String question, int limit) {
        return search(lesson, question, null, limit);
    }

    public static List<Hit> search(SkillLesson lesson, String question, String focusSlug, int limit) {
        Query query = Query.parse(question);
        List<Hit> hits = collect(lesson, query, focusSlug);
        if (hits.isEmpty() && lesson != null && lesson.getConcepts() != null && !lesson.getConcepts().isEmpty()) {
            hits.add(toHit(lesson, lesson.getConcepts().getFirst(), 1));
        }
        return top(hits, limit);
    }

    public static List<Hit> searchAll(String question, int limit) {
        Query query = Query.parse(question);
        List<Hit> hits = new ArrayList<>();
        for (SkillCurriculum.Outline outline : SkillCurriculum.all()) {
            hits.addAll(collect(lessonOf(outline), query, null));
        }
        return top(hits, limit);
    }

    public static List<Hit> searchOrWiden(SkillLesson lesson, String question, String focusSlug, int limit) {
        Query query = Query.parse(question);
        List<Hit> local = collect(lesson, query, focusSlug);
        int best = local.isEmpty() ? 0 : local.getFirst().score();
        if (best >= 16) {
            return top(local, limit);
        }
        List<Hit> library = new ArrayList<>(local);
        for (SkillCurriculum.Outline outline : SkillCurriculum.all()) {
            if (lesson != null && outline.key().equals(lesson.getKey())) {
                continue;
            }
            library.addAll(collect(lessonOf(outline), query, null));
        }
        if (library.isEmpty()) {
            return search(lesson, question, focusSlug, limit);
        }
        return top(library, limit);
    }

    public static List<Hit> searchOrWiden(SkillLesson lesson, String question, int limit) {
        return searchOrWiden(lesson, question, null, limit);
    }

    private static SkillLesson lessonOf(SkillCurriculum.Outline outline) {
        SkillLesson lesson = new SkillLesson();
        lesson.setKey(outline.key());
        lesson.setName(outline.name());
        lesson.setSummary(outline.summary());
        lesson.setConcepts(outline.concepts());
        lesson.setStandards(SkillPlaybook.standards(outline.key()));
        lesson.setPatterns(SkillPlaybook.patterns(outline.key()));
        return lesson;
    }

    private static List<Hit> collect(SkillLesson lesson, Query query, String focusSlug) {
        List<Hit> hits = new ArrayList<>();
        if (lesson == null || query.terms().isEmpty()) {
            return hits;
        }
        if (lesson.getConcepts() != null) {
            for (CoreConcept concept : lesson.getConcepts()) {
                if (concept == null) {
                    continue;
                }
                int score = scoreConcept(concept, query, focusSlug);
                if (score > 0) {
                    hits.add(toHit(lesson, concept, score));
                }
            }
        }
        if (lesson.getPatterns() != null) {
            for (PatternRule rule : lesson.getPatterns()) {
                if (rule == null) {
                    continue;
                }
                String hay = rule.getName() + " " + rule.getIntent() + " " + nullToEmpty(rule.getHow());
                int score = weightedOverlap(query, hay, rule.getName());
                if (query.wantsPattern()) {
                    score += 2;
                }
                if (score >= 8) {
                    String how = rule.getHow() == null || rule.getHow().isBlank() ? "" : "\n- " + rule.getHow();
                    hits.add(new Hit(lesson.getKey(), lesson.getName(), "",
                            rule.getName(),
                            rule.getIntent() + "\n- " + rule.getIntent() + how,
                            score, "pattern"));
                }
            }
        }
        if (lesson.getStandards() != null) {
            for (StandardRule rule : lesson.getStandards()) {
                if (rule == null) {
                    continue;
                }
                String hay = rule.getTitle() + " " + rule.getRule() + " " + nullToEmpty(rule.getWhy());
                int score = weightedOverlap(query, hay, rule.getTitle());
                if (query.wantsStandard()) {
                    score += 2;
                }
                if (score >= 8) {
                    String body = rule.getRule()
                            + (rule.getWhy() == null || rule.getWhy().isBlank() ? "" : "\nWhy: " + rule.getWhy());
                    hits.add(new Hit(lesson.getKey(), lesson.getName(), "",
                            rule.getTitle(), body, score, "standard"));
                }
            }
        }
        hits.sort(Comparator.comparingInt(Hit::score).reversed());
        return hits;
    }

    private static List<Hit> top(List<Hit> hits, int limit) {
        if (hits.isEmpty()) {
            return List.of();
        }
        hits.sort(Comparator.comparingInt(Hit::score).reversed());
        int best = hits.getFirst().score();
        List<Hit> strong = new ArrayList<>();
        for (Hit hit : hits) {
            if (hit.score() * 2 >= best && hit.score() >= 6) {
                strong.add(hit);
            }
        }
        if (strong.isEmpty()) {
            strong.add(hits.getFirst());
        }
        int cap = limit < 1 ? 3 : Math.min(limit, strong.size());
        return new ArrayList<>(strong.subList(0, cap));
    }

    private static Hit toHit(SkillLesson lesson, CoreConcept concept, int score) {
        StringBuilder body = new StringBuilder();
        if (concept.getWhy() != null && !concept.getWhy().isBlank()) {
            body.append(concept.getWhy()).append('\n');
        }
        if (concept.getPoints() != null) {
            for (String point : concept.getPoints()) {
                body.append("- ").append(point).append('\n');
            }
        }
        if (concept.getTrap() != null && !concept.getTrap().isBlank()) {
            body.append("Trap: ").append(concept.getTrap()).append('\n');
        }
        if (concept.getTakeaway() != null && !concept.getTakeaway().isBlank()) {
            body.append("Takeaway: ").append(concept.getTakeaway()).append('\n');
        }
        if (concept.getUseCase() != null && !concept.getUseCase().isBlank()) {
            body.append("Production: ").append(concept.getUseCase()).append('\n');
        }
        if (concept.getInterviews() != null) {
            for (InterviewCard card : concept.getInterviews()) {
                if (card == null || card.getQuestion() == null) {
                    continue;
                }
                body.append("Q: ").append(card.getQuestion()).append('\n');
                if (card.getAnswer() != null) {
                    body.append("A: ").append(card.getAnswer()).append('\n');
                }
            }
        }
        return new Hit(lesson.getKey(), lesson.getName(), concept.getSlug(), concept.getTitle(), body.toString(), score,
                "concept");
    }

    private static int scoreConcept(CoreConcept concept, Query query, String focusSlug) {
        int score = weightedOverlap(query, concept.getTitle() + " " + concept.getSlug(), concept.getTitle()) * 2;
        score += weightedOverlap(query, concept.getWhy(), "") / 2;
        score += weightedOverlap(query, concept.getDepth(), "") / 3;
        if (concept.getPoints() != null) {
            score += weightedOverlap(query, String.join(" ", concept.getPoints()), "");
        }
        if (concept.getTrap() != null) {
            score += weightedOverlap(query, concept.getTrap(), "") / 2;
        }
        if (concept.getInterviews() != null) {
            for (InterviewCard card : concept.getInterviews()) {
                if (card != null) {
                    score += weightedOverlap(query, card.getQuestion() + " " + card.getAnswer(), card.getQuestion());
                }
            }
        }
        if (focusSlug != null && !focusSlug.isBlank() && focusSlug.equals(concept.getSlug())) {
            score += 10;
        }
        return score;
    }

    static int weightedOverlap(Query query, String haystack, String title) {
        if (query.terms().isEmpty() || haystack == null || haystack.isBlank()) {
            return 0;
        }
        String lower = haystack.toLowerCase(Locale.ROOT);
        String titleLower = title == null ? "" : title.toLowerCase(Locale.ROOT);
        int score = 0;
        if (!query.phrase().isBlank() && query.phrase().length() >= 8 && lower.contains(query.phrase())) {
            score += 20;
            if (titleLower.contains(query.phrase())) {
                score += 12;
            }
        }
        int matched = 0;
        for (String term : query.terms()) {
            if (!lower.contains(term)) {
                continue;
            }
            matched++;
            int weight = term.length() >= 10 ? 5 : term.length() >= 6 ? 3 : 1;
            if (titleLower.contains(term)) {
                weight *= 2;
            }
            score += weight;
        }
        if (matched == 0) {
            return 0;
        }
        return score;
    }

    static List<String> tokens(String text) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9+]+")) {
            if (raw.length() < 3 || STOP.contains(raw)) {
                continue;
            }
            out.add(raw);
        }
        return new ArrayList<>(out);
    }

    static List<String> expand(List<String> terms) {
        LinkedHashSet<String> out = new LinkedHashSet<>(terms);
        for (String term : terms) {
            List<String> extra = EXPAND.get(term);
            if (extra != null) {
                out.addAll(extra);
            }
        }
        return new ArrayList<>(out);
    }

    static String phraseOf(List<String> terms) {
        List<String> distinctive = new ArrayList<>();
        for (String term : terms) {
            if (term.length() >= 4 && !STOP.contains(term)) {
                distinctive.add(term);
            }
        }
        if (distinctive.size() >= 2) {
            return distinctive.get(0) + " " + distinctive.get(1);
        }
        return distinctive.isEmpty() ? "" : distinctive.getFirst();
    }

    static int overlap(List<String> terms, String haystack) {
        return weightedOverlap(new Query(terms, phraseOf(terms), false, false), haystack, "");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final Set<String> STOP = Set.of(
            "the", "and", "for", "that", "this", "with", "from", "your", "what", "when",
            "how", "why", "are", "can", "does", "into", "about", "please", "explain",
            "use", "using", "used", "java", "jdk", "openjdk", "spring", "give", "tell",
            "need", "should", "would", "could", "there", "they", "them", "than", "then",
            "also", "just", "like", "some", "more", "very", "have", "been", "will",
            "each", "both", "only", "such", "make", "made", "here", "mean", "means",
            "difference", "between", "vs");

    private static final Map<String, List<String>> EXPAND = Map.ofEntries(
            Map.entry("dip", List.of("dependency", "inversion")),
            Map.entry("srp", List.of("single", "responsibility")),
            Map.entry("ocp", List.of("open", "closed")),
            Map.entry("lsp", List.of("liskov", "substitution")),
            Map.entry("isp", List.of("interface", "segregation")),
            Map.entry("chm", List.of("concurrenthashmap")),
            Map.entry("hashcode", List.of("equals", "hashcode")),
            Map.entry("equals", List.of("equals", "hashcode")),
            Map.entry("optional", List.of("optional", "orelse", "orelseget")),
            Map.entry("virtual", List.of("virtual", "threads", "pinning")),
            Map.entry("rag", List.of("retrieve", "generate", "evidence")));
}
