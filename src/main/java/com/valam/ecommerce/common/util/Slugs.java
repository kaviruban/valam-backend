package com.valam.ecommerce.common.util;

import java.text.Normalizer;
import java.util.Locale;

/** Builds URL slugs such as "cold-pressed-groundnut-oil" from display names. */
public final class Slugs {

    private static final int MAX_LENGTH = 180;

    private Slugs() {
    }

    /**
     * Lowercase ASCII letters and digits joined by single hyphens. Characters outside a-z/0-9 (including
     * non-Latin scripts such as Tamil) are dropped, so the result can be empty; callers supply a fallback.
     */
    public static String slugify(String input) {
        String ascii = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }
}
