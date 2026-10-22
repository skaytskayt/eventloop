package com.maxbot.eventLoop.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Живой запрос к Nominatim.
 *
 * <p>Вынесен за пределы обычного прогона: требует сети и может падать по
 * причинам, к коду отношения не имеющим. Запуск:
 *
 * <pre>./mvnw test -Dgeocodercheck=true -Dtest=NominatimGeocoderLiveTest</pre>
 *
 * <p>Прогонять перед демо — если Nominatim сменит формат ответа или начнёт
 * отклонять наш User-Agent, бот перестанет находить адреса.
 */
@EnabledIfSystemProperty(named = "geocodercheck", matches = "true")
class NominatimGeocoderLiveTest {

    @Test
    @DisplayName("«Казань, Баумана 10» находится в центре Казани")
    void findsKazanAddress() {
        Optional<AddressGeocoder.Place> place = new NominatimGeocoder().geocode("Казань, Баумана 10");
        assertThat(place).isPresent();
        assertThat(Haversine.distanceKm(place.get().lat(), place.get().lon(), 55.7961, 49.1064)).isLessThan(5);
        assertThat(place.get().name()).contains("Баумана");
    }
}
