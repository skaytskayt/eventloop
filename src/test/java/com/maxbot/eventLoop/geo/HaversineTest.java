package com.maxbot.eventLoop.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HaversineTest {

    // Координаты станций из справочника hh.ru
    private static final double OKHOTNY_LAT = 55.757273;
    private static final double OKHOTNY_LON = 37.615425;
    private static final double VDNH_LAT = 55.819626;
    private static final double VDNH_LON = 37.640751;

    @Test
    @DisplayName("Охотный Ряд → ВДНХ ≈ 7 км")
    void knownDistance() {
        double km = Haversine.distanceKm(OKHOTNY_LAT, OKHOTNY_LON, VDNH_LAT, VDNH_LON);
        assertThat(km).isBetween(6.5, 7.5);
    }

    @Test
    @DisplayName("расстояние до самой себя — ноль")
    void zeroDistance() {
        assertThat(Haversine.distanceKm(OKHOTNY_LAT, OKHOTNY_LON, OKHOTNY_LAT, OKHOTNY_LON))
                .isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("формула симметрична")
    void symmetric() {
        double forward = Haversine.distanceKm(OKHOTNY_LAT, OKHOTNY_LON, VDNH_LAT, VDNH_LON);
        double backward = Haversine.distanceKm(VDNH_LAT, VDNH_LON, OKHOTNY_LAT, OKHOTNY_LON);
        assertThat(forward).isCloseTo(backward, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("перепутанные местами координаты дают правдоподобный, но неверный результат")
    void swappedCoordinatesAreSilentlyWrong() {
        double correct = Haversine.distanceKm(OKHOTNY_LAT, OKHOTNY_LON, VDNH_LAT, VDNH_LON);
        double swapped = Haversine.distanceKm(OKHOTNY_LON, OKHOTNY_LAT, VDNH_LON, VDNH_LAT);

        // Именно поэтому своп нельзя ловить взглядом на число: оно выглядит нормально.
        assertThat(swapped).isNotCloseTo(correct, org.assertj.core.data.Offset.offset(0.5));
        assertThat(swapped).isBetween(1.0, 10.0);
    }

    @Test
    @DisplayName("инвариант отличает московские координаты от перепутанных и нулевых")
    void moscowInvariant() {
        assertThat(Haversine.looksLikeMoscow(OKHOTNY_LAT, OKHOTNY_LON)).isTrue();
        assertThat(Haversine.looksLikeMoscow(OKHOTNY_LON, OKHOTNY_LAT)).isFalse();
        assertThat(Haversine.looksLikeMoscow(0, 0)).isFalse();
        assertThat(Haversine.looksLikeMoscow(59.93, 30.33)).isFalse(); // Санкт-Петербург
    }
}
