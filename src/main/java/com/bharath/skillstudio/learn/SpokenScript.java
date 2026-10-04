package com.bharath.skillstudio.learn;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns trainer copy into something a TTS engine can speak without reading code or commands.
 */
public final class SpokenScript {

    private static final Pattern FENCE = Pattern.compile("(?s)```.*?```");
    private static final Pattern TICKS = Pattern.compile("`[^`]*`");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern CVE = Pattern.compile("(?i)\\bCVE-\\d{4}-\\d+\\b");
    private static final Pattern TICKET = Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b");
    private static final Pattern ISSUE = Pattern.compile("#\\d+");
    private static final Pattern API_KEY = Pattern.compile("(?i)\\b[a-z0-9_]*api[_-]?key\\b");
    private static final Pattern CAMEL = Pattern.compile("(?<=[a-z])([A-Z])");
    private static final Pattern ACRONYM = Pattern.compile("(?<=[A-Z])([A-Z][a-z])");
    private static final Pattern DOTTED_ID = Pattern.compile("(?<=[A-Za-z])\\.(?=[A-Za-z])");

    private SpokenScript() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = FENCE.matcher(raw).replaceAll(" ");
        text = TICKS.matcher(text).replaceAll(" ");
        text = URL.matcher(text).replaceAll(" ");
        text = CVE.matcher(text).replaceAll("a security advisory");
        text = TICKET.matcher(text).replaceAll(" ");
        text = ISSUE.matcher(text).replaceAll(" ");
        text = API_KEY.matcher(text).replaceAll("the API key");
        StringBuilder kept = new StringBuilder();
        for (String sentence : text.split("(?<=[.!?])\\s+|\\n+")) {
            if (looksLikeCode(sentence) || looksLikeCommand(sentence)) {
                continue;
            }
            String spoken = humanize(sentence);
            if (!spoken.isBlank()) {
                kept.append(spoken).append(' ');
            }
        }
        return kept.toString().replaceAll("\\s+", " ").trim();
    }

    static boolean looksLikeCode(String sentence) {
        if (sentence == null) {
            return true;
        }
        String text = sentence.trim();
        if (text.length() < 2) {
            return true;
        }
        if (text.contains("{") || text.contains("};") || text.contains("=>")) {
            return true;
        }
        if (text.contains("(") && text.contains(")") && text.matches(".*[A-Za-z0-9]\\.[A-Za-z0-9].*\\(.*")) {
            return true;
        }
        if (text.matches(".*\\w+\\.\\w+\\.\\w+.*=.*")) {
            return true;
        }
        if (text.contains("();") || text.startsWith("//")) {
            return true;
        }
        return false;
    }

    static boolean looksLikeCommand(String sentence) {
        if (sentence == null) {
            return true;
        }
        String text = sentence.trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("--")) {
            return true;
        }
        return text.matches("^(curl|kubectl|mvn|docker|git|npm|helm|aws|http)\\b.*");
    }

    static String humanize(String sentence) {
        String text = sentence.replace('_', ' ');
        text = CAMEL.matcher(text).replaceAll(" $1");
        text = ACRONYM.matcher(text).replaceAll(" $1");
        text = DOTTED_ID.matcher(text).replaceAll(" ");
        return text.replaceAll("\\s+", " ").trim();
    }
}
