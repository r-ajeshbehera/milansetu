package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.response.MatchSuggestion.Factor;
import com.rajesh.milansetu.entity.ClothingColor;
import com.rajesh.milansetu.entity.Gender;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic weighted scoring (max 100). Pure Java, no Spring: every boundary is unit-tested.
 *
 * | Factor                     | Points                         |
 * | Same gender (hard filter)  | 25 — different gender = no match |
 * | Age difference ≤ 2 / ≤ 5   | 25 / 12                        |
 * | Same clothing colour       | 20                             |
 * | Distance ≤ 1 km / ≤ 3 km   | 20 / 10  (Haversine)           |
 * | Found within 2 h / 6 h     | 10 / 5   (full timestamps)     |
 * Only the highest applicable step per factor counts. Boundaries are inclusive.
 */
public final class MatchScorer {
    public static final int THRESHOLD = 50;

    private MatchScorer() { }

    public record Person(Gender gender, int age, ClothingColor clothing, double lat, double lng, LocalDateTime at) { }

    public record Result(int score, List<Factor> factors, double distanceKm, long minutesApart) { }

    /** Empty = genders differ (hard filter). */
    public static Optional<Result> score(Person missing, Person found) {
        if (missing.gender() != found.gender()) return Optional.empty();
        List<Factor> factors = new ArrayList<>();
        int total = add(factors, "GENDER", 25, "same gender: " + missing.gender());

        int ageDiff = Math.abs(missing.age() - found.age());
        if (ageDiff <= 2) total += add(factors, "AGE", 25, "age difference " + ageDiff + " (≤ 2)");
        else if (ageDiff <= 5) total += add(factors, "AGE", 12, "age difference " + ageDiff + " (≤ 5)");
        else factors.add(new Factor("AGE", 0, "age difference " + ageDiff));

        if (missing.clothing() == found.clothing()) total += add(factors, "CLOTHING", 20, "both " + missing.clothing());
        else factors.add(new Factor("CLOTHING", 0, missing.clothing() + " vs " + found.clothing()));

        double km = GeoUtils.haversineKm(missing.lat(), missing.lng(), found.lat(), found.lng());
        String kmText = String.format("%.2f km", km);
        if (km <= 1.0) total += add(factors, "DISTANCE", 20, kmText + " (≤ 1 km)");
        else if (km <= 3.0) total += add(factors, "DISTANCE", 10, kmText + " (≤ 3 km)");
        else factors.add(new Factor("DISTANCE", 0, kmText));

        long minutes = Duration.between(missing.at(), found.at()).toMinutes();
        if (minutes < 0) factors.add(new Factor("TIME", 0, "found before last seen (" + minutes + " min)"));
        else if (minutes <= 120) total += add(factors, "TIME", 10, minutes + " min after last seen (≤ 2 h)");
        else if (minutes <= 360) total += add(factors, "TIME", 5, minutes + " min after last seen (≤ 6 h)");
        else factors.add(new Factor("TIME", 0, minutes + " min after last seen"));

        return Optional.of(new Result(total, factors, GeoUtils.round2(km), minutes));
    }

    public static boolean isSuggestion(int score) {
        return score >= THRESHOLD;
    }

    private static int add(List<Factor> factors, String name, int points, String detail) {
        factors.add(new Factor(name, points, detail));
        return points;
    }
}
