package com.rajesh.milansetu.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Geographic helpers. */
public final class GeoUtils {
    /** Mean Earth radius in kilometres. */
    public static final double EARTH_RADIUS_KM = 6371.0;
    private static final double KM_PER_DEGREE_LAT = 111.32;

    private GeoUtils() { }

    /**
     * Haversine great-circle distance:
     * a = sin²(Δφ/2) + cos φ1 · cos φ2 · sin²(Δλ/2);  d = 2R · atan2(√a, √(1−a))
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record BoundingBox(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng) { }

    /** A cheap rectangle that contains the radius circle, so the DB can use simple BETWEEN filters first. */
    public static BoundingBox boundingBox(double lat, double lng, double radiusKm) {
        double dLat = radiusKm / KM_PER_DEGREE_LAT;
        double cos = Math.max(Math.cos(Math.toRadians(lat)), 0.01);
        double dLng = radiusKm / (KM_PER_DEGREE_LAT * cos);
        return new BoundingBox(bd(lat - dLat), bd(lat + dLat), bd(lng - dLng), bd(lng + dLng));
    }

    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
    }
}
