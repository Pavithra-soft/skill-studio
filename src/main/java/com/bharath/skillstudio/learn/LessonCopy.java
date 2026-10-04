package com.bharath.skillstudio.learn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Makes a concept look like a good tutor answer: one line, bullets, trap, takeaway.
 * Does not mutate the authored catalog object.
 */
public final class LessonCopy {

    private LessonCopy() {
    }

    public static CoreConcept polish(CoreConcept source) {
        CoreConcept copy = copy(source);
        if (copy.getPoints().isEmpty()) {
            copy.setPoints(extractPoints(copy));
        }
        if (copy.getTrap() == null || copy.getTrap().isBlank()) {
            copy.setTrap(extractTrap(copy));
        }
        if (copy.getTakeaway() == null || copy.getTakeaway().isBlank()) {
            copy.setTakeaway(firstSentence(copy.getWhy(), 180));
        }
        return copy;
    }

    public static CoreConcept copy(CoreConcept source) {
        if (source == null) {
            return new CoreConcept();
        }
        CoreConcept copy = new CoreConcept(
                source.getKind(),
                source.getTitle(),
                source.getWhy(),
                source.getExample(),
                source.getUseCase());
        copy.setSlug(source.getSlug());
        copy.setDepth(source.getDepth());
        copy.setTrap(source.getTrap());
        copy.setTakeaway(source.getTakeaway());
        copy.setGenerated(source.isGenerated());
        copy.setPoints(new ArrayList<>(source.getPoints() == null ? List.of() : source.getPoints()));
        List<InterviewCard> interviews = new ArrayList<>();
        if (source.getInterviews() != null) {
            interviews.addAll(source.getInterviews());
        }
        copy.setInterviews(interviews);
        List<CodeSample> samples = new ArrayList<>();
        if (source.getSamples() != null) {
            samples.addAll(source.getSamples());
        }
        copy.setSamples(samples);
        return copy;
    }

    public static List<String> extractPoints(CoreConcept concept) {
        List<String> out = new ArrayList<>();
        String why = firstSentence(concept.getWhy(), 180);
        if (!why.isBlank()) {
            out.add(why);
        }
        for (String paragraph : paragraphs(concept.getDepth())) {
            for (String sentence : sentences(paragraph)) {
                String clipped = clip(sentence, 180);
                if (clipped.isBlank() || clipped.length() < 24 || containsIgnoreCase(out, clipped)) {
                    continue;
                }
                out.add(clipped);
                if (out.size() == 8) {
                    return out;
                }
            }
        }
        if (out.size() < 4 && concept.getUseCase() != null && !concept.getUseCase().isBlank()) {
            out.add("In production: " + firstSentence(concept.getUseCase(), 160));
        }
        return out;
    }

    static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        String trimmed = text.replace('\n', ' ').trim();
        int start = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c != '.' && c != '!' && c != '?') {
                continue;
            }
            if (i + 1 < trimmed.length() && !Character.isWhitespace(trimmed.charAt(i + 1))) {
                continue;
            }
            String sentence = trimmed.substring(start, i + 1).trim();
            if (!sentence.isBlank()) {
                out.add(sentence);
            }
            start = i + 1;
        }
        if (start < trimmed.length()) {
            String tail = trimmed.substring(start).trim();
            if (!tail.isBlank()) {
                out.add(tail);
            }
        }
        return out;
    }

    static String clip(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() > max) {
            return trimmed.substring(0, max).trim() + "…";
        }
        return trimmed;
    }

    static String extractTrap(CoreConcept concept) {
        for (String paragraph : paragraphs(concept.getDepth())) {
            String lower = paragraph.toLowerCase(Locale.ROOT);
            if (lower.contains("trap") || lower.startsWith("never ") || lower.startsWith("do not ")
                    || lower.contains("the trap") || lower.contains("fail open")
                    || lower.contains("do not ")) {
                return firstSentence(paragraph, 220);
            }
        }
        return "";
    }

    static List<String> paragraphs(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (String part : text.split("\\n\\s*\\n")) {
            String trimmed = part.replace('\n', ' ').trim();
            if (!trimmed.isBlank()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    static String firstSentence(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        int end = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                if (i + 1 == trimmed.length() || Character.isWhitespace(trimmed.charAt(i + 1))) {
                    end = i + 1;
                    break;
                }
            }
        }
        String sentence = end > 0 ? trimmed.substring(0, end).trim() : trimmed;
        if (sentence.length() > max) {
            return sentence.substring(0, max).trim() + "…";
        }
        return sentence;
    }

    private static boolean containsIgnoreCase(List<String> items, String candidate) {
        String needle = candidate.toLowerCase(Locale.ROOT);
        for (String item : items) {
            if (item.toLowerCase(Locale.ROOT).equals(needle)) {
                return true;
            }
        }
        return false;
    }
}
