package com.bharath.skillstudio.ai;

import com.bharath.skillstudio.learn.LibrarySearch.Hit;
import com.bharath.skillstudio.learn.LibrarySearch.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns ranked catalog hits into a short tutor reply: definition, bullets, trap, takeaway.
 */
public final class TutorAnswer {

    private TutorAnswer() {
    }

    public static String compose(String question, List<Hit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "I do not have a catalog card for that yet. Pick a concept and generate it, or ask about a listed huddle.";
        }
        Hit best = hits.getFirst();
        Card card = Card.from(best);
        StringBuilder out = new StringBuilder();
        String definition = card.definition;
        if (definition.isBlank()) {
            definition = best.title() + " is in the catalog for this skill.";
        }
        out.append(definition).append("\n\n");
        int bullets = 0;
        for (String point : card.points) {
            if (point.isBlank() || containsIgnoreCase(definition, point)) {
                continue;
            }
            out.append("• ").append(clip(point, 220)).append('\n');
            bullets++;
            if (bullets == 6) {
                break;
            }
        }
        if (bullets == 0 && !card.bodyLine.isBlank() && !containsIgnoreCase(definition, card.bodyLine)) {
            out.append("• ").append(clip(card.bodyLine, 220)).append('\n');
        }
        if (!card.trap.isBlank()) {
            out.append("Watch-out. ").append(clip(card.trap, 220)).append('\n');
        }
        if (!card.takeaway.isBlank()) {
            out.append("Takeaway. ").append(clip(card.takeaway, 220)).append('\n');
        }
        if (!card.production.isBlank()) {
            out.append("In production. ").append(clip(card.production, 240)).append('\n');
        }
        if (!card.answer.isBlank() && relevant(question, card.question + " " + card.answer)) {
            out.append('\n').append("Interview angle. ").append(clip(card.answer, 260)).append('\n');
        }
        Hit related = relatedHit(hits, best, question);
        if (related != null) {
            Card extra = Card.from(related);
            String line = extra.definition.isBlank() ? extra.bodyLine : extra.definition;
            if (!line.isBlank()) {
                out.append('\n').append("Also · ").append(related.title()).append(". ").append(clip(line, 180));
            }
        }
        return out.toString().strip();
    }

    public static String grade(String user, String catalogAnswer, String followUp) {
        Query query = Query.parse(user);
        Query expected = Query.parse(catalogAnswer);
        int matched = 0;
        for (String term : expected.terms()) {
            if (term.length() < 5) {
                continue;
            }
            if (query.terms().contains(term) || (user != null && user.toLowerCase(Locale.ROOT).contains(term))) {
                matched++;
            }
        }
        int needed = 0;
        for (String term : expected.terms()) {
            if (term.length() >= 5) {
                needed++;
            }
        }
        double ratio = needed == 0 ? 0 : matched / (double) needed;
        StringBuilder out = new StringBuilder();
        if (ratio >= 0.35) {
            out.append("On track. You hit the catalog terms.\n\n");
        } else if (ratio >= 0.15) {
            out.append("Partly. Tighten it against the catalog answer.\n\n");
        } else {
            out.append("Not yet. Here is the catalog answer to steal from.\n\n");
        }
        out.append(catalogAnswer);
        if (followUp != null && !followUp.isBlank()) {
            out.append("\n\nFollow-up they may ask: ").append(followUp);
        }
        return out.toString();
    }

    public static String voiceOf(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int lines = 0;
        for (String line : answer.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.startsWith("• ")) {
                trimmed = trimmed.substring(2);
            }
            out.append(trimmed).append(' ');
            lines++;
            if (lines == 8) {
                break;
            }
        }
        return out.toString().trim();
    }

    static Card card(Hit hit) {
        return Card.from(hit);
    }

    private static Hit relatedHit(List<Hit> hits, Hit best, String question) {
        if (hits.size() < 2) {
            return null;
        }
        Hit second = hits.get(1);
        if (second.score() * 2 < best.score()) {
            return null;
        }
        if (second.title().equalsIgnoreCase(best.title())) {
            return null;
        }
        if (!relevant(question, second.title() + " " + second.body())) {
            return null;
        }
        return second;
    }

    private static boolean relevant(String question, String haystack) {
        Query query = Query.parse(question);
        if (query.terms().isEmpty() || haystack == null) {
            return false;
        }
        String lower = haystack.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String term : query.terms()) {
            if (term.length() >= 5 && lower.contains(term)) {
                hits++;
            }
        }
        return hits >= 1 || (!query.phrase().isBlank() && lower.contains(query.phrase()));
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null
                && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT).substring(0,
                Math.min(needle.length(), 40)));
    }

    private static String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max).trim() + "…";
    }

    record Card(String definition, List<String> points, String trap, String takeaway, String production,
                String question, String answer, String bodyLine) {
        static Card from(Hit hit) {
            String definition = "";
            String trap = "";
            String takeaway = "";
            String production = "";
            String question = "";
            String answer = "";
            String bodyLine = "";
            List<String> points = new ArrayList<>();
            if (hit == null || hit.body() == null) {
                return new Card("", List.of(), "", "", "", "", "", "");
            }
            for (String raw : hit.body().split("\n")) {
                String line = raw.trim();
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("- ")) {
                    points.add(stripLabel(line.substring(2)));
                } else if (starts(line, "Trap:")) {
                    trap = stripLabel(line.substring(5));
                } else if (starts(line, "Takeaway:")) {
                    takeaway = stripLabel(line.substring(9));
                } else if (starts(line, "Production:")) {
                    production = stripLabel(line.substring(11));
                } else if (starts(line, "Why:")) {
                    if (definition.isBlank()) {
                        definition = stripLabel(line.substring(4));
                    }
                } else if (starts(line, "Q:")) {
                    if (question.isBlank()) {
                        question = stripLabel(line.substring(2));
                    }
                } else if (starts(line, "A:")) {
                    if (answer.isBlank()) {
                        answer = stripLabel(line.substring(2));
                    }
                } else if (definition.isBlank()) {
                    definition = line;
                } else if (bodyLine.isBlank()) {
                    bodyLine = line;
                }
            }
            return new Card(definition, points, trap, takeaway, production, question, answer, bodyLine);
        }

        private static boolean starts(String line, String prefix) {
            return line.regionMatches(true, 0, prefix, 0, prefix.length());
        }

        private static String stripLabel(String text) {
            String trimmed = text.trim();
            if (trimmed.startsWith("What:")) {
                return trimmed.substring(5).trim();
            }
            return trimmed;
        }
    }
}
