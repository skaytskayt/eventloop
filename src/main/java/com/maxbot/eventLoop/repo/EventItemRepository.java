package com.maxbot.eventLoop.repo;

import com.maxbot.eventLoop.domain.EventItem;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventItemRepository extends JpaRepository<EventItem, Long> {

    Optional<EventItem> findByExternalKey(String externalKey);

    /**
     * Мероприятие и его сеанс одним выражением.
     *
     * <p>CTE нужен, чтобы не спрашивать у базы id между двумя вставками:
     * повторный импорт обновляет существующие записи, а не плодит дубликаты,
     * и всё это без промежуточного round-trip.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            WITH upserted_event AS (
                INSERT INTO event_item (external_key, title, summary, category, age_rating,
                                        venue_id, poster_url, event_url, ticket_url)
                VALUES (:key, :title, :summary, :category, :age, :venueId,
                        :poster, :eventUrl, :ticketUrl)
                ON CONFLICT (external_key) DO UPDATE SET
                    title      = EXCLUDED.title,
                    summary    = EXCLUDED.summary,
                    category   = EXCLUDED.category,
                    venue_id   = EXCLUDED.venue_id,
                    poster_url = EXCLUDED.poster_url,
                    event_url  = EXCLUDED.event_url,
                    ticket_url = EXCLUDED.ticket_url
                RETURNING id
            )
            INSERT INTO event_session (event_id, starts_at, price_rub)
            SELECT id, :startsAt, :price FROM upserted_event
            ON CONFLICT (event_id, starts_at) DO UPDATE SET price_rub = EXCLUDED.price_rub
            """, nativeQuery = true)
    void upsertEventWithSession(@Param("key") String externalKey,
                                @Param("title") String title,
                                @Param("summary") String summary,
                                @Param("category") String category,
                                @Param("age") String ageRating,
                                @Param("venueId") Long venueId,
                                @Param("poster") String posterUrl,
                                @Param("eventUrl") String eventUrl,
                                @Param("ticketUrl") String ticketUrl,
                                @Param("startsAt") Instant startsAt,
                                @Param("price") int priceRub);

    long countByExternalKeyStartingWith(String prefix);
}
