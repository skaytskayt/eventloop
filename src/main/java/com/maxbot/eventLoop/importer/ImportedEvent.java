package com.maxbot.eventLoop.importer;

import com.maxbot.eventLoop.domain.Category;
import java.time.LocalDateTime;

/**
 * Мероприятие, вычитанное из внешнего источника, до сопоставления с нашей моделью.
 * Координат здесь нет — источник их не отдаёт, их добавляет {@link VenueResolver}.
 */
public record ImportedEvent(
        String title,
        String venueName,
        LocalDateTime startsAt,
        int priceRub,
        Category category,
        String sourceCategory,
        String posterUrl,
        String ticketUrl
) {

    /** Ключ идемпотентности: повторный импорт не должен плодить дубликаты. */
    public String externalKey() {
        String slug = ticketUrl.replaceAll("^https?://[^/]+", "")
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return "tl-" + (slug.length() > 100 ? slug.substring(0, 100) : slug);
    }
}
