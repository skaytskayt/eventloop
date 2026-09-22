package com.maxbot.eventLoop.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Сопоставление названий площадок.
 *
 * <p>Исходная ошибка, ради которой существуют эти тесты: в Java {@code \b}
 * опирается на ASCII-класс {@code \w} и на кириллице не срабатывает, поэтому
 * вся нормализация молча ничего не делала — 134 мероприятия из 162 отбрасывались.
 */
class VenueResolverTest {

    @ParameterizedTest(name = "«{0}» ≡ «{1}»")
    @CsvSource({
            "'Театр имени Евгения Вахтангова',             'Театр имени Евг. Вахтангова'",
            "'Симоновская сцена Театра имени Евгения Вахтангова', 'Театр имени Евг. Вахтангова'",
            "'Новая сцена и Арт-кафе театра Вахтангова',   'Театр имени Евг. Вахтангова'",
            "'Театр Вахтангова (экскурсии)',               'Театр имени Евг. Вахтангова'",
            "'МХТ имени А. П. Чехова',                     'МХТ имени А. П. Чехова'",
            "'Театр «Современник»',                        'Московский театр «Современник»'",
            "'Государственный исторический музей',         'Государственный исторический музей'"
    })
    @DisplayName("варианты написания и филиалы сводятся к одной площадке")
    void matchesVariants(String source, String ours) {
        Set<String> a = VenueResolver.significantTokens(source);
        Set<String> b = VenueResolver.significantTokens(ours);
        assertThat(a).as("различающие слова источника: %s", a).isNotEmpty();
        assertThat(a.stream().anyMatch(b::contains))
                .as("«%s» должно сопоставиться с «%s» (%s vs %s)", source, ours, a, b)
                .isTrue();
    }

    @ParameterizedTest(name = "«{0}» ≠ «{1}»")
    @CsvSource({
            "'Театр на Таганке',        'Московский театр на Юго-Западе'",
            "'Театр сатиры',            'Московский драматический театр имени Н. В. Гоголя'",
            "'Большой театр',           'Малый театр'",
            "'Дарвиновский музей',      'Еврейский музей и центр толерантности'",
            "'Московский планетарий',   'Музей космонавтики'"
    })
    @DisplayName("разные площадки не склеиваются служебными словами")
    void doesNotMatchDifferentVenues(String a, String b) {
        Set<String> ta = VenueResolver.significantTokens(a);
        Set<String> tb = VenueResolver.significantTokens(b);
        assertThat(ta.stream().anyMatch(tb::contains))
                .as("«%s» и «%s» не должны совпасть (%s vs %s)", a, b, ta, tb)
                .isFalse();
    }

    @Test
    @DisplayName("короткое имя собственное находит длинное официальное название")
    void shortProperNameMatchesLongOfficialOne() {
        // «Сац» короче обычного порога различающего слова, и набор у источника
        // получается из одного короткого слова, а у нас — из длинных.
        Set<String> source = VenueResolver.significantTokens("Театр имени Сац");
        Set<String> ours = VenueResolver.collect("Детский музыкальный театр имени Н. И. Сац", 3);
        assertThat(source).as("различающие слова источника").containsExactly("сац");
        assertThat(ours.stream().anyMatch(source::contains))
                .as("наш набор %s должен содержать «сац»", ours)
                .isTrue();
    }

    @Test
    @DisplayName("голое число не считается различающим словом")
    void digitsAreNotSignificant() {
        assertThat(VenueResolver.significantTokens("Сцена 2")).doesNotContain("2");
        assertThat(VenueResolver.significantTokens("123 456")).isEmpty();
    }

    @Test
    @DisplayName("служебные слова отбрасываются — иначе совпадёт всё со всем")
    void dropsStopwords() {
        assertThat(VenueResolver.significantTokens("Государственный академический театр имени"))
                .as("из одних служебных слов различающих не остаётся")
                .isEmpty();
    }

    @Test
    @DisplayName("нормализация работает на кириллице, а не молча пропускает её")
    void normalizationActuallyRuns() {
        // Ровно то, что было сломано: строки различаются только оформлением.
        assertThat(VenueResolver.normalize("Театр «Современник»"))
                .isEqualTo(VenueResolver.normalize("театр Современник"));
        assertThat(VenueResolver.normalize("Молодёжный")).isEqualTo(VenueResolver.normalize("Молодежный"));
    }

    @Test
    @DisplayName("пустое и мусорное не ломают разбор")
    void handlesEmpty() {
        assertThat(VenueResolver.significantTokens("")).isEmpty();
        assertThat(VenueResolver.significantTokens("   ")).isEmpty();
        assertThat(VenueResolver.significantTokens("!!! ??? 123")).isEmpty();
    }
}
