package com.rajesh.milansetu.service;

import java.util.Locale;

/** "  Laxmi   NAYAK " -> "laxmi nayak". Used for exact-but-forgiving name/village matching. */
public final class TextNormalizer {
    private TextNormalizer() { }

    public static String normalize(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
