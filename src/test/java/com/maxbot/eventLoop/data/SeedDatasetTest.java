package com.maxbot.eventLoop.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.geo.Haversine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Проверка справочников, которые ведутся вручную: площадок и станций метро.
 *
 * <p>Афиша сюда не входит — она приходит импортом и проверяется в
 * {@code TicketlandParserTest}. А вот координаты площадок вбиваются руками,
 * и ошибка в них не видна глазом: перепутанные местами широта и долгота дают
 * правдоподобное, но неверное расстояние. Такое ловится только инвариантом.
 */
class SeedDatasetTest {

    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");

    private record Point(String name, double lat, double lon) {
    }

    private static List<Point> venues;
    private static List<Point> metro;

    @BeforeAll
    static void parse() throws IOException {
        venues = new ArrayList<>();
        for (String file : List.of("R__seed_venues_base.sql", "R__seed_venues_import.sql")) {
            String sql = read(file);
            Matcher m = Pattern.compile(
                            "\\((\\d+), '((?:[^']|'')*)', '((?:[^']|'')*)', ([-\\d.]+), ([-\\d.]+)\\)")
                    .matcher(sql);
            while (m.find()) {
                venues.add(new Point(m.group(2).replace("''", "'"),
                        Double.parseDouble(m.group(4)), Double.parseDouble(m.group(5))));
            }
        }

        metro = new ArrayList<>();
        Matcher m = Pattern.compile(
                        "INSERT INTO metro_station \\(name, lat, lon\\) VALUES \\('((?:[^']|'')*)', "
                                + "([-\\d.]+), ([-\\d.]+)\\)")
                .matcher(read("R__seed_metro.sql"));
        while (m.find()) {
            metro.add(new Point(m.group(1).replace("''", "'"),
                    Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))));
        }
    }

    @Test
    @DisplayName("справочники разобраны и не пусты")
    void parsed() {
        assertThat(venues).as("площадки").hasSizeGreaterThanOrEqualTo(40);
        assertThat(metro).as("станции метро").hasSizeGreaterThanOrEqualTo(300);
    }

    @Test
    @DisplayName("координаты площадок проходят московский инвариант")
    void venueCoordinatesAreSane() {
        for (Point v : venues) {
            assertThat(Haversine.looksLikeMoscow(v.lat(), v.lon()))
                    .as("координаты «%s»: %s, %s", v.name(), v.lat(), v.lon())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("координаты станций метро проходят тот же инвариант")
    void metroCoordinatesAreSane() {
        for (Point s : metro) {
            assertThat(s.lat() > s.lon())
                    .as("станция «%s»: %s, %s", s.name(), s.lat(), s.lon())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("названия площадок уникальны — иначе сопоставление при импорте неоднозначно")
    void venueNamesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (Point v : venues) {
            assertThat(seen.add(v.name())).as("площадка «%s» встречается дважды", v.name()).isTrue();
        }
    }

    @Test
    @DisplayName("две площадки не стоят в одной точке — признак скопированных координат")
    void venuesAreNotStacked() {
        for (int i = 0; i < venues.size(); i++) {
            for (int j = i + 1; j < venues.size(); j++) {
                Point a = venues.get(i);
                Point b = venues.get(j);
                if (a.lat() == b.lat() && a.lon() == b.lon()) {
                    // Совпадение допустимо только у площадок одного здания.
                    assertThat(a.name()).as("«%s» и «%s» имеют одинаковые координаты",
                            a.name(), b.name()).isNotEqualTo(b.name());
                }
            }
        }
    }

    @Test
    @DisplayName("площадки разнесены по городу, а не сгрудились в центре")
    void venuesAreSpread() {
        double minLat = venues.stream().mapToDouble(Point::lat).min().orElseThrow();
        double maxLat = venues.stream().mapToDouble(Point::lat).max().orElseThrow();
        double minLon = venues.stream().mapToDouble(Point::lon).min().orElseThrow();
        double maxLon = venues.stream().mapToDouble(Point::lon).max().orElseThrow();
        assertThat(Haversine.distanceKm(minLat, minLon, maxLat, maxLon))
                .as("разброс площадок по городу")
                .isGreaterThan(10.0);
    }

    @Test
    @DisplayName("демонстрационные мероприятия удаляются, а не показываются рядом с настоящими")
    void demoEventsAreRemoved() throws IOException {
        assertThat(Files.exists(MIGRATIONS.resolve("R__seed_events.sql")))
                .as("файл с выдуманной афишей удалён")
                .isFalse();
        assertThat(read("R__seed_venues_base.sql"))
                .as("старые демо-записи вычищаются из базы")
                .contains("DELETE FROM event_item WHERE external_key LIKE 'seed-%'");
    }

    private static String read(String name) throws IOException {
        Path path = MIGRATIONS.resolve(name);
        assertThat(Files.exists(path)).as("миграция %s существует", name).isTrue();
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
