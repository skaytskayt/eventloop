package com.maxbot.eventLoop.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.domain.Category;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Проверка разбора афиши на сохранённой странице источника.
 *
 * <p>Сеть здесь не нужна намеренно: у источника антибот-защита, и тест,
 * ходящий в интернет, падал бы по чужим причинам. Фикстура — реальные
 * блоки JSON-LD, снятые со страницы Пушкинской карты.
 */
class TicketlandParserTest {

    private static final TicketlandParser PARSER = new TicketlandParser();
    private static List<ImportedEvent> events;

    @BeforeAll
    static void parseFixture() throws IOException {
        try (InputStream in = TicketlandParserTest.class
                .getResourceAsStream("/fixtures/ticketland-pushkin-card.html")) {
            assertThat(in).as("фикстура найдена").isNotNull();
            events = PARSER.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("со страницы вычитываются мероприятия")
    void parsesEvents() {
        assertThat(events).hasSizeGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("у каждого мероприятия есть всё, что нужно карточке")
    void everyEventIsComplete() {
        for (ImportedEvent e : events) {
            assertThat(e.title()).as("название").isNotBlank();
            assertThat(e.venueName()).as("площадка у «%s»", e.title()).isNotBlank();
            assertThat(e.startsAt()).as("дата у «%s»", e.title()).isNotNull();
            assertThat(e.ticketUrl()).as("ссылка на билет у «%s»", e.title())
                    .startsWith("https://");
            assertThat(e.category()).as("категория у «%s»", e.title()).isNotNull();
        }
    }

    /**
     * Ключевое свойство источника: цена приходит числом.
     * Из «от 400 ₽» остаток баланса посчитать нельзя, и продукт теряет смысл.
     */
    @Test
    @DisplayName("цена — точное число, а не «от N рублей»")
    void pricesAreExact() {
        assertThat(events).allSatisfy(e ->
                assertThat(e.priceRub())
                        .as("цена «%s»", e.title())
                        .isBetween(1, 100_000));
    }

    @Test
    @DisplayName("относительные ссылки разворачиваются в абсолютные")
    void urlsAreAbsolute() {
        assertThat(events).allSatisfy(e -> {
            assertThat(e.ticketUrl()).doesNotStartWith("//").startsWith("https://");
            if (e.posterUrl() != null) {
                assertThat(e.posterUrl()).doesNotStartWith("//").startsWith("https://");
            }
        });
    }

    @Test
    @DisplayName("ключ идемпотентности уникален и стабилен")
    void externalKeysAreUnique() {
        List<String> keys = events.stream().map(ImportedEvent::externalKey).toList();
        assertThat(keys).doesNotHaveDuplicates();
        assertThat(keys).allSatisfy(k -> assertThat(k).startsWith("tl-").hasSizeLessThan(128));
    }

    @ParameterizedTest(name = "«{0}» → {1}")
    @CsvSource({
            "Спектакли,      THEATRE",
            "Мюзиклы,        THEATRE",
            "Детям,          THEATRE",
            "Концерты,       CONCERT",
            "Классика,       CONCERT",
            "Экскурсии,      EXCURSION",
            "Кино,           CINEMA",
            "Выставки,       MUSEUM",
            "Музеи,          MUSEUM"
    })
    @DisplayName("категории источника сводятся к нашим")
    void mapsCategories(String source, Category expected) {
        assertThat(TicketlandParser.mapCategory(source, null)).isEqualTo(expected);
    }

    @Test
    @DisplayName("неизвестная категория не теряет мероприятие")
    void unknownCategoryFallsBack() {
        assertThat(TicketlandParser.mapCategory("Нечто невиданное", null))
                .isEqualTo(Category.THEATRE);
        assertThat(TicketlandParser.mapCategory(null, null)).isEqualTo(Category.THEATRE);
    }

    @ParameterizedTest(name = "цена «{0}»")
    @CsvSource({
            "400,     400",
            "'1 200', 1200",
            "'400.00',400",
            "'400,50',400"
    })
    void parsesPrice(String raw, int expected) {
        assertThat(TicketlandParser.price(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "бесплатно", "от", "0"})
    @DisplayName("непригодная цена отбрасывается, а не превращается в ноль")
    void rejectsUnusablePrice(String raw) {
        assertThat(TicketlandParser.price(raw)).isNull();
    }

    @Test
    @DisplayName("мусор на входе не роняет разбор")
    void survivesGarbage() {
        assertThat(PARSER.parse(null)).isEmpty();
        assertThat(PARSER.parse("")).isEmpty();
        assertThat(PARSER.parse("<html><body>нет никакого JSON</body></html>")).isEmpty();
        assertThat(PARSER.parse(
                "<script type=\"application/ld+json\">{сломано</script>")).isEmpty();
    }

    @Test
    @DisplayName("разные площадки, а не одна и та же на всю страницу")
    void venuesAreVaried() {
        assertThat(events.stream().map(ImportedEvent::venueName).distinct().count())
                .isGreaterThanOrEqualTo(3);
    }
}
