package com.maxbot.eventLoop.geo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Геокодер на публичном Nominatim (OpenStreetMap).
 *
 * <p>Политика сервиса: свой User-Agent с контактом и не чаще одного запроса
 * в секунду с приложения — иначе IP банят. Поэтому {@link #geocode} общий
 * для всего приложения и synchronized: бин один, и пауза между запросами
 * держится глобально.
 *
 * <p>Ни текст запроса, ни найденные координаты в лог не пишутся: адрес может
 * быть домашним.
 */
@Component
public class NominatimGeocoder implements AddressGeocoder {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocoder.class);

    static final String BASE =
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=ru&accept-language=ru&q=";
    static final String USER_AGENT = "EventLoop-bot/1.0 (+https://max.ru/t560_hakaton_max_bot)";
    static final Duration TIMEOUT = Duration.ofSeconds(5);
    /** Политика Nominatim: не чаще 1 rps (как PAUSE_NOMINATIM_MS в tools/feed/build-feed.mjs). */
    static final long MIN_INTERVAL_MS = 1100;
    /**
     * addresstype Nominatim, при котором найден целый город или регион.
     * По place_rank не отличить: у Москвы-субъекта он 8, а у её районов
     * («Арбат», «Хамовники») 14 — меньше, чем у Казани-города (16).
     */
    static final Set<String> COARSE_TYPES = Set.of(
            "country", "state", "region", "province", "county", "city", "municipality");
    /** Длина колонки origin_name. */
    static final int MAX_NAME = 128;

    private static final Pattern POSTCODE = Pattern.compile("^\\d{5,6}$");
    private static final Pattern HOUSE_NUMBER = Pattern.compile("^\\d+[\\p{L}\\d/-]*$");
    private static final Pattern MARKUP = Pattern.compile("[*_`\\[\\]]");
    private static final List<String> CITIES = List.of("Москва", "Казань");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private long lastRequestNanos;
    private boolean requested;

    @Override
    public synchronized Optional<Place> geocode(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            throttle();
            HttpRequest request = HttpRequest.newBuilder(searchUri(query.trim()))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("Nominatim ответил HTTP {}", response.statusCode());
                return Optional.empty();
            }
            return parse(response.body(), query.trim());
        } catch (IOException e) {
            log.warn("Nominatim недоступен: {}", e.getClass().getSimpleName());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /** Ждёт остаток интервала с прошлого запроса и запоминает момент нового. */
    private void throttle() throws InterruptedException {
        if (requested) {
            long elapsedMs = (System.nanoTime() - lastRequestNanos) / 1_000_000;
            if (elapsedMs < MIN_INTERVAL_MS) {
                Thread.sleep(MIN_INTERVAL_MS - elapsedMs);
            }
        }
        lastRequestNanos = System.nanoTime();
        requested = true;
    }

    static URI searchUri(String query) {
        return URI.create(BASE + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    /** Первое место из ответа jsonv2; пусто — ответ пустой, битый или с невозможными координатами. */
    static Optional<Place> parse(String json, String fallbackName) {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null || !root.isArray() || root.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = root.get(0);
            JsonNode latNode = first.get("lat");
            JsonNode lonNode = first.get("lon");
            if (latNode == null || lonNode == null || !latNode.isValueNode() || !lonNode.isValueNode()) {
                return Optional.empty();
            }
            double lat = Double.parseDouble(latNode.asString());
            double lon = Double.parseDouble(lonNode.asString());
            if (!Double.isFinite(lat) || !Double.isFinite(lon)
                    || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return Optional.empty();
            }
            JsonNode display = first.get("display_name");
            String displayName = display == null || display.isNull() ? null : display.asString();
            JsonNode type = first.get("addresstype");
            boolean coarse = type != null && type.isValueNode() && COARSE_TYPES.contains(type.asString());
            return Optional.of(new Place(shortName(displayName, fallbackName), lat, lon, coarse));
        } catch (RuntimeException e) {
            // JacksonException непроверяемое, NumberFormatException тоже.
            return Optional.empty();
        }
    }

    /**
     * Короткий адрес из display_name: улица и дом (дом после улицы), плюс
     * город, если это Москва или Казань. Округ, индекс и страна отбрасываются.
     */
    static String shortName(String displayName, String fallback) {
        String name;
        if (displayName == null || displayName.isBlank()) {
            name = fallback == null ? "" : fallback;
        } else {
            List<String> parts = new ArrayList<>(Arrays.stream(displayName.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .filter(p -> !POSTCODE.matcher(p).matches())
                    .filter(p -> !p.equals("Россия"))
                    .filter(p -> !p.contains("федеральный округ"))
                    .toList());
            if (parts.size() >= 2 && HOUSE_NUMBER.matcher(parts.get(0)).matches()) {
                String house = parts.get(0);
                parts.set(0, parts.get(1));
                parts.set(1, house);
            }
            List<String> picked = new ArrayList<>(parts.subList(0, Math.min(2, parts.size())));
            for (String part : parts.subList(Math.min(2, parts.size()), parts.size())) {
                if (CITIES.contains(part) && !picked.contains(part)) {
                    picked.add(part);
                    break;
                }
            }
            name = String.join(", ", picked);
        }
        name = MARKUP.matcher(name).replaceAll("").replaceAll("\\s+", " ").trim();
        if (name.length() > MAX_NAME) {
            name = name.substring(0, MAX_NAME - 1) + "…";
        }
        return name;
    }
}
