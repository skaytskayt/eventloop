package com.maxbot.eventLoop.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.domain.Category;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Проверка подборки Mini App — того же сорта, что {@code SeedDatasetTest} для
 * площадок: данные ведутся руками, и ошибка в них не видна глазом.
 *
 * <p>Полсотни карточек вбиты вручную, у каждой свой файл фотографии. Опечатка
 * в пути даёт не исключение, а пустой прямоугольник на карточке — заметить его
 * можно только долистав до неё. Этим и занимается тест.
 *
 * <p>JS-раннера в проекте нет, поэтому {@code data.js} разбирается регулярками.
 * Это годится ровно потому, что файл писался по одному шаблону: он данные, а
 * не программа.
 */
class MiniAppDatasetTest {

    private static final Path APP = Path.of("src", "main", "resources", "static", "app");

    private record Card(int sessionId, int eventId, String title, String category,
                        String photo, int priceRub, String ticketUrl, String eventUrl) {
    }

    private static List<Card> cards;

    @BeforeAll
    static void parse() throws IOException {
        String js = Files.readString(APP.resolve("data.js"), StandardCharsets.UTF_8);
        cards = new ArrayList<>();
        // Границы записи — от sessionId до закрывающей скобки объекта.
        Matcher m = Pattern.compile("\\{\\s*sessionId:.*?\\n {2}\\}", Pattern.DOTALL).matcher(js);
        while (m.find()) {
            String body = m.group();
            cards.add(new Card(
                    Integer.parseInt(field(body, "sessionId")),
                    Integer.parseInt(field(body, "eventId")),
                    quoted(body, "title"),
                    quoted(body, "category"),
                    quoted(body, "photo"),
                    Integer.parseInt(field(body, "priceRub")),
                    quoted(body, "ticketUrl"),
                    quoted(body, "eventUrl")));
        }
    }

    private static String field(String body, String name) {
        return match(body, name + ":\\s*([\\d.]+)");
    }

    private static String quoted(String body, String name) {
        return match(body, name + ":\\s*'((?:[^']|\\\\')*)'");
    }

    private static String match(String body, String regex) {
        Matcher m = Pattern.compile(regex).matcher(body);
        if (!m.find()) {
            throw new IllegalStateException("в карточке нет поля по шаблону " + regex);
        }
        return m.group(1);
    }

    @Test
    @DisplayName("подборка разобрана и в ней полсотни с лишним карточек")
    void datasetIsParsed() {
        assertThat(cards).hasSizeGreaterThanOrEqualTo(40);
    }

    @Test
    @DisplayName("у каждой карточки лежит свой файл фотографии")
    void everyPhotoFileExists() {
        assertThat(cards).allSatisfy(card ->
                assertThat(APP.resolve(card.photo()))
                        .as("фото карточки «%s»", card.title())
                        .isRegularFile());
    }

    @Test
    @DisplayName("фотографии не повторяются: иначе лента выглядит как одно событие")
    void photosAreDistinct() {
        assertThat(cards).extracting(Card::photo).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("идентификаторы уникальны")
    void identifiersAreUnique() {
        assertThat(cards).extracting(Card::sessionId).doesNotHaveDuplicates();
        assertThat(cards).extracting(Card::eventId).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("категория — из тех же, что знает бот")
    void categoriesMatchBackend() {
        List<String> known = Stream.of(Category.values()).map(Enum::name).toList();
        assertThat(cards).extracting(Card::category).isSubsetOf(known);
    }

    /** Кошельки Mini App: 2000 ₽ на кино, 3000 ₽ на остальное. */
    @Test
    @DisplayName("ни один билет не дороже своего кошелька — иначе его нельзя купить в принципе")
    void everyTicketFitsItsWallet() {
        assertThat(cards).allSatisfy(card -> {
            int limit = "CINEMA".equals(card.category()) ? 2000 : 3000;
            assertThat(card.priceRub())
                    .as("цена карточки «%s»", card.title())
                    .isPositive()
                    .isLessThanOrEqualTo(limit);
        });
    }

    /**
     * Ссылка ведёт на страницу самого мероприятия, а не на поиск или на
     * главную оператора: именно на ней человек доводит покупку до конца, и
     * без неё подтверждение покупки в Mini App проверять не на чем.
     */
    @Test
    @DisplayName("ссылка ведёт на страницу мероприятия у оператора")
    void everyCardLinksToItsEventPage() {
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.ticketUrl())
                    .as("ссылка карточки «%s»", card.title())
                    .startsWith("https://www.ticketland.ru/")
                    .endsWith("/")
                    .doesNotContain("?");
            // Путь вида /раздел/площадка/мероприятие/ — три сегмента.
            assertThat(card.ticketUrl().replace("https://www.ticketland.ru/", "").split("/"))
                    .as("путь карточки «%s»", card.title())
                    .hasSize(3);
        });
    }

    @Test
    @DisplayName("у каждой фотографии в assets указан источник")
    void everyPhotoIsCredited() throws IOException {
        String credits = Files.readString(APP.resolve("assets/events/CREDITS.md"), StandardCharsets.UTF_8);
        assertThat(cards).allSatisfy(card -> {
            String file = card.photo().substring(card.photo().lastIndexOf('/') + 1);
            assertThat(credits).as("строка про %s в CREDITS.md", file).contains(file);
        });
    }
}
