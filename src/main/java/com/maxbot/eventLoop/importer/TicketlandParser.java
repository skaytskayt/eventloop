package com.maxbot.eventLoop.importer;

import com.maxbot.eventLoop.domain.Category;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Разбор афиши Пушкинской карты с ticketland.ru.
 *
 * <p>Парсится не вёрстка, а блоки <a href="https://schema.org/Event">schema.org/Event</a>
 * в формате JSON-LD. Вёрстку меняют при каждом редизайне, структурированные данные —
 * гораздо реже, потому что от них зависит выдача поисковиков.
 *
 * <p>Класс намеренно без зависимостей на сеть и базу: на вход строка HTML,
 * на выход список записей. Так его можно проверить на сохранённой странице.
 */
public class TicketlandParser {

    private static final Logger log = LoggerFactory.getLogger(TicketlandParser.class);

    private static final Pattern LD_JSON = Pattern.compile(
            "<script[^>]*type\\s*=\\s*[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.DOTALL);

    private final ObjectMapper mapper = new ObjectMapper();

    public List<ImportedEvent> parse(String html) {
        List<ImportedEvent> events = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return events;
        }
        Matcher m = LD_JSON.matcher(html);
        int malformed = 0;
        while (m.find()) {
            try {
                JsonNode node = mapper.readTree(m.group(1).trim());
                if ("Event".equals(text(node, "@type"))) {
                    toEvent(node).ifPresent(events::add);
                }
            } catch (RuntimeException e) {
                malformed++;
            }
        }
        if (malformed > 0) {
            log.debug("Пропущено нечитаемых блоков JSON-LD: {}", malformed);
        }
        return events;
    }

    private Optional<ImportedEvent> toEvent(JsonNode node) {
        String title = text(node, "name");
        JsonNode location = node.get("location");
        JsonNode offers = node.get("offers");
        if (title == null || location == null || offers == null) {
            return Optional.empty();
        }

        String venue = text(location, "name");
        String ticketUrl = absolute(text(offers, "url"));
        String startRaw = text(node, "startDate");
        Integer price = price(text(offers, "price"));
        if (venue == null || ticketUrl == null || startRaw == null || price == null) {
            return Optional.empty();
        }

        LocalDateTime startsAt;
        try {
            startsAt = LocalDateTime.parse(startRaw);
        } catch (DateTimeParseException e) {
            log.debug("Непонятная дата «{}» у «{}»", startRaw, title);
            return Optional.empty();
        }

        String sourceCategory = text(node, "additionalType");
        return Optional.of(new ImportedEvent(
                title.trim(),
                venue.trim(),
                startsAt,
                price,
                mapCategory(sourceCategory, text(node, "keywords")),
                sourceCategory,
                absolute(text(node, "image")),
                ticketUrl));
    }

    /**
     * Категории источника сводятся к нашим. Неопознанное уходит в театр:
     * на афише билетного оператора это самый вероятный вариант, и лучше
     * показать мероприятие не в той категории, чем потерять его совсем.
     */
    static Category mapCategory(String additionalType, String keywords) {
        String s = ((additionalType == null ? "" : additionalType) + " "
                + (keywords == null ? "" : keywords)).toLowerCase(Locale.ROOT);
        if (s.contains("кино") || s.contains("фильм")) {
            return Category.CINEMA;
        }
        if (s.contains("музе") || s.contains("выставк") || s.contains("галере")) {
            return Category.MUSEUM;
        }
        if (s.contains("концерт") || s.contains("филармон") || s.contains("опера")
                || s.contains("балет") || s.contains("классика")) {
            return Category.CONCERT;
        }
        if (s.contains("экскурс") || s.contains("лекц") || s.contains("мастер-класс")) {
            return Category.EXCURSION;
        }
        return Category.THEATRE;
    }

    /** Цена приходит строкой: «400», «1 200», изредка с копейками. */
    static Integer price(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^\\d.,]", "").replaceAll("[.,]\\d{1,2}$", "")
                .replaceAll("[^\\d]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(digits);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Ссылки в источнике бывают относительными и без схемы: «//media…» и «/teatry/…». */
    static String absolute(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String u = url.trim();
        if (u.startsWith("//")) {
            return "https:" + u;
        }
        if (u.startsWith("/")) {
            return "https://www.ticketland.ru" + u;
        }
        return u;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
