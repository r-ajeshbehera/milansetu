package com.rajesh.milansetu.service;

import com.rajesh.milansetu.entity.HelpDesk;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Finds the nearest active desk to a point (Haversine). Pure helper, unit-tested. */
public final class DeskLocator {
    private DeskLocator() { }

    public record Nearest(HelpDesk desk, double distanceKm) { }

    public static Optional<Nearest> nearest(List<HelpDesk> desks, double lat, double lng) {
        return desks.stream()
                .filter(HelpDesk::isActive)
                .map(d -> new Nearest(d, GeoUtils.haversineKm(lat, lng, d.getLatitude().doubleValue(), d.getLongitude().doubleValue())))
                .min(Comparator.comparingDouble(Nearest::distanceKm));
    }
}
