package com.maxbot.eventLoop.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Разбор ответа Nominatim без сети. Ошибка здесь видна человеку сразу: бот
 * пишет «Нашёл: …» с полным адресом до самой России или падает на сохранении
 * имени длиннее колонки.
 */
class NominatimGeocoderTest {

    private static String fixture() throws IOException {
        try (InputStream in = NominatimGeocoderTest.class.getResourceAsStream("/fixtures/nominatim-search.json")) {
            assertThat(in).as("фикстура найдена").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("ответ на «Казань, Баумана 10» — короткий адрес и координаты")
    void parsesFixture() throws IOException {
        assertThat(NominatimGeocoder.parse(fixture(), "x"))
                .contains(new AddressGeocoder.Place("улица Баумана, 7/10, Казань", 55.7934295, 49.1098114));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "[]",
            "<html>oops",
            "{}",
            "[{\"lat\": \"abc\", \"lon\": \"49.1\", \"display_name\": \"Казань\"}]",
            "[{\"lat\": \"95\", \"lon\": \"49.1\", \"display_name\": \"Казань\"}]",
            "[{\"lat\": \"55.7\", \"lon\": \"190\", \"display_name\": \"Казань\"}]",
            "[{\"display_name\": \"Казань\"}]"
    })
    @DisplayName("пустой, битый или странный ответ — пусто, без исключения")
    void badResponsesAreEmpty(String json) {
        assertThat(NominatimGeocoder.parse(json, "x")).isEmpty();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "район Арбат, Москва, Центральный федеральный округ, Россия | район Арбат, Москва",
            "7, 4-я Тверская-Ямская улица, 49, Тверской район, Москва, Центральный федеральный округ, 125047, Россия | 4-я Тверская-Ямская улица, 7, Москва",
            "МЕГА Химки, 1, Ленинградское шоссе, микрорайон ИКЕА, Новокуркино, Химки, городской округ Химки, Московская область, Центральный федеральный округ, 141410, Россия | МЕГА Химки, 1",
            "Привет, 56-й квартал, Московский район, Казань, городской округ Казань, Татарстан, Приволжский федеральный округ, Россия | Привет, 56-й квартал, Казань",
            "7/10, улица Баумана, Старо-Татарская слобода, Вахитовский район, Казань, городской округ Казань, Татарстан, Приволжский федеральный округ, 420111, Россия | улица Баумана, 7/10, Казань"
    })
    @DisplayName("короткое имя: улица и дом, город — без округа, индекса и страны")
    void shortName(String displayName, String expected) {
        assertThat(NominatimGeocoder.shortName(displayName, "x")).isEqualTo(expected);
    }

    @Test
    @DisplayName("символы разметки MARKDOWN из имени вырезаются")
    void markdownCharsRemoved() {
        String name = NominatimGeocoder.shortName("*улица_ [Баумана]`, 10, Казань, Россия", "x");
        assertThat(name).doesNotContain("*", "_", "`", "[", "]").isEqualTo("улица Баумана, 10, Казань");
    }

    @Test
    @DisplayName("длинное имя обрезается до длины колонки origin_name")
    void longNameTruncated() {
        String name = NominatimGeocoder.shortName("а".repeat(300) + ", Москва, Россия", "x");
        assertThat(name).hasSize(128).endsWith("…");
    }

    @Test
    @DisplayName("без display_name берётся запрос — с той же очисткой и обрезкой")
    void fallbackName() {
        assertThat(NominatimGeocoder.shortName(null, "Москва, Арбат")).isEqualTo("Москва, Арбат");
        assertThat(NominatimGeocoder.shortName("  ", "*Москва_  [Арбат]`")).isEqualTo("Москва Арбат");
        assertThat(NominatimGeocoder.shortName("", "б".repeat(200))).hasSize(128).endsWith("…");
    }

    @Test
    @DisplayName("запрос кодируется в параметр q фиксированного адреса поиска")
    void searchUri() {
        URI uri = NominatimGeocoder.searchUri("Казань, Баумана 10");
        assertThat(uri.toString())
                .startsWith("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1"
                        + "&countrycodes=ru&accept-language=ru&q=")
                .doesNotContain(" ")
                .doesNotContainPattern("[а-яА-ЯёЁ]");
        assertThat(uri.toASCIIString()).isEqualTo(uri.toString());
    }
}
