package com.maxbot.eventLoop.importer;

import com.maxbot.eventLoop.domain.Venue;
import com.maxbot.eventLoop.repo.EventItemRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Импорт афиши Пушкинской карты.
 *
 * <p>Источник — раздел Пушкинской карты у билетного оператора: в нём по построению
 * только те мероприятия, которые по карте оплатить можно, и цена указана числом,
 * а не строкой «от 400 ₽». Последнее принципиально: из «от 400» невозможно
 * посчитать остаток баланса, а это главное обещание бота.
 *
 * <p>У источника стоит антибот-защита, поэтому импорт — операция по требованию
 * с паузами между страницами, а не фоновый опрос. Если однажды защита сработает,
 * бот продолжит работать на том, что уже в базе.
 */
@Service
public class EventImportService {

    private static final Logger log = LoggerFactory.getLogger(EventImportService.class);

    private static final String LISTING = "https://www.ticketland.ru/pushkin-card/";
    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    /** Пауза между страницами: вежливость к источнику и защита от блокировки. */
    private static final long PAGE_DELAY_MS = 1500;

    private final TicketlandParser parser = new TicketlandParser();
    private final VenueResolver venueResolver;
    private final EventItemRepository events;
    private final TransactionTemplate transactions;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public EventImportService(VenueResolver venueResolver, EventItemRepository events,
                              PlatformTransactionManager transactionManager) {
        this.venueResolver = venueResolver;
        this.events = events;
        // Явная транзакция вместо @Transactional: загрузка страниц не должна
        // держать соединение с базой, а вызов метода того же бина аннотацию
        // всё равно бы не увидел.
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Итог импорта — то, что стоит показать человеку, а не просто «ок». */
    public record Report(int pagesFetched,
                         int parsed,
                         int imported,
                         int skippedUnknownVenue,
                         int skippedPast,
                         List<String> unknownVenues,
                         List<String> errors) {
    }

    public Report importFromSource(int pages) {
        List<ImportedEvent> all = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int fetched = 0;

        for (int page = 1; page <= pages; page++) {
            String url = page == 1 ? LISTING : LISTING + "?page=" + page;
            try {
                String html = fetch(url);
                List<ImportedEvent> parsed = parser.parse(html);
                if (parsed.isEmpty()) {
                    log.info("Страница {} не дала мероприятий — останавливаюсь", page);
                    break;
                }
                all.addAll(parsed);
                fetched++;
                log.info("Страница {}: получено мероприятий {}", page, parsed.size());
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                errors.add("страница " + page + ": " + e.getMessage());
                log.warn("Страница {} не загрузилась: {}", page, e.getMessage());
                break;
            }
            pause();
        }
        return store(all, fetched, errors);
    }

    /** Разбор заранее сохранённой страницы — на случай, когда источник закрылся. */
    public Report importFromHtml(String html) {
        return store(parser.parse(html), 1, List.of());
    }

    private Report store(List<ImportedEvent> parsed, int pagesFetched, List<String> errors) {
        TreeSet<String> unknownVenues = new TreeSet<>();
        int imported = 0;
        int skippedPast = 0;
        int skippedVenue = 0;

        LocalDateTime now = LocalDateTime.now(MOSCOW);

        for (ImportedEvent e : parsed) {
            if (e.startsAt().isBefore(now)) {
                skippedPast++;
                continue;
            }
            Optional<Venue> venue = venueResolver.resolve(e.venueName());
            if (venue.isEmpty()) {
                unknownVenues.add(e.venueName());
                skippedVenue++;
                continue;
            }
            final ImportedEvent event = e;
            final Long venueId = venue.get().getId();
            transactions.executeWithoutResult(status -> events.upsertEventWithSession(
                    event.externalKey(),
                    event.title(),
                    summary(event),
                    event.category().name(),
                    "6+",
                    venueId,
                    event.posterUrl() == null ? fallbackPoster(event) : event.posterUrl(),
                    event.ticketUrl(),
                    event.ticketUrl(),
                    event.startsAt().atZone(MOSCOW).toInstant(),
                    event.priceRub()));
            imported++;
        }

        Report report = new Report(pagesFetched, parsed.size(), imported,
                skippedVenue, skippedPast, List.copyOf(unknownVenues), errors);
        log.info("Импорт завершён: страниц {}, разобрано {}, загружено {}, "
                        + "пропущено (нет площадки) {}, пропущено (прошедшие) {}",
                report.pagesFetched(), report.parsed(), report.imported(),
                report.skippedUnknownVenue(), report.skippedPast());
        if (!report.unknownVenues().isEmpty()) {
            log.info("Неизвестные площадки — добавьте их с координатами, чтобы забрать эти мероприятия:\n  {}",
                    String.join("\n  ", report.unknownVenues()));
        }
        return report;
    }

    private static String summary(ImportedEvent e) {
        String source = e.sourceCategory() == null ? "" : e.sourceCategory() + ". ";
        return (source + e.venueName()).trim();
    }

    private static String fallbackPoster(ImportedEvent e) {
        return "https://placehold.co/900x600/2d3561/ffffff.png?text="
                + java.net.URLEncoder.encode(e.category().title(),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private String fetch(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(url))
                        .timeout(TIMEOUT)
                        .header("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                        + "(KHTML, like Gecko) Chrome/140.0 Safari/537.36")
                        .header("Accept-Language", "ru-RU,ru;q=0.9")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 403) {
            throw new IOException("источник закрыл доступ (403) — сработала антибот-защита");
        }
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static void pause() {
        try {
            Thread.sleep(PAGE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
