package com.maxbot.eventLoop.geo;

/**
 * Расстояние по большому кругу. Дублирует формулу, которая считается в SQL, —
 * здесь она нужна для тестов и для подсказок при вводе станции метро.
 */
public final class Haversine {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private Haversine() {
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /**
     * В Москве широта всегда больше долготы (55.7 против 37.6). Перепутанные
     * местами координаты дают правдоподобное, но неверное расстояние, поэтому
     * инвариант проверяется явно, а не подразумевается.
     */
    public static boolean looksLikeMoscow(double lat, double lon) {
        return lat > lon && lat > 55.4 && lat < 56.1 && lon > 36.9 && lon < 38.0;
    }
}
