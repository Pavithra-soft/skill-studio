package com.bharath.skillstudio.learn;

import java.util.Locale;

public final class ConceptSlug {

    private ConceptSlug() {
    }

    public static String of(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        return title.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
