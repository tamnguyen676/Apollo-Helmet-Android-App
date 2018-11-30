package com.apollo.apollo;

import com.here.android.mpa.common.GeoCoordinate;

import java.util.Locale;

public interface DistanceCalculator {
    default double toMiles(double meters) {
        return meters * 0.000621371;
    }

    default String getDistanceMiles(GeoCoordinate c1, GeoCoordinate c2) {
        double distance = toMiles(c1.distanceTo(c2));

        if (distance < .1) {
            distance *= 5280;
            return String.format(Locale.US, "%.0f ft", distance);
        }

        return String.format(Locale.US, "%.1f mi", distance);
    }

    default double toMeters(double miles) {
        return miles / 0.000621371;
    }

}
