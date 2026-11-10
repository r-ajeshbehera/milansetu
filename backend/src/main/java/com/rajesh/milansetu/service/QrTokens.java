package com.rajesh.milansetu.service;

import java.util.Optional;
import java.util.UUID;

/** QR payload format: "MILANSETU:<uuid>". Scanners may also send the bare UUID (manual entry). */
public final class QrTokens {
    public static final String PREFIX = "MILANSETU:";

    private QrTokens() { }

    public static String payload(String token) {
        return PREFIX + token;
    }

    /** Returns the normalised UUID string, or empty for anything that is not a valid token. */
    public static Optional<String> parse(String raw) {
        if (raw == null) return Optional.empty();
        String value = raw.trim();
        if (value.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) value = value.substring(PREFIX.length());
        try {
            UUID uuid = UUID.fromString(value);
            return uuid.toString().equalsIgnoreCase(value) ? Optional.of(uuid.toString()) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
