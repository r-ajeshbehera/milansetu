package com.rajesh.milansetu.service;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/** Six digits from a cryptographically secure generator; leading zeros kept ("004512"). */
public final class OtpGenerator {
    private static final SecureRandom SECURE = new SecureRandom();

    private OtpGenerator() { }

    public static String generate() {
        return generate(SECURE);
    }

    public static String generate(RandomGenerator random) {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
