package com.maxbot.eventLoop.miniapp;

import com.maxbot.eventLoop.config.AdminIds;
import com.maxbot.eventLoop.dialog.Screens;
import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.domain.DialogState;
import com.maxbot.eventLoop.geo.AddressGeocoder;
import com.maxbot.eventLoop.geo.Haversine;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Mini App: реакции пользователя, журнал действий и статистика для администраторов.
 *
 * <p>Пользователь определяется только по подписанному initData из заголовка
 * {@code X-Init-Data}. Сам заголовок не логируется: там подпись и имя.
 */
@RestController
public class MiniAppController {

    private static final Logger log = LoggerFactory.getLogger(MiniAppController.class);
    private static final String INIT_DATA = "X-Init-Data";

    private final InitDataVerifier verifier;
    private final MiniAppStore store;
    private final AdminIds admins;
    private final AppUserRepository users;
    private final AddressGeocoder geocoder;
    private final Screens screens;

    public MiniAppController(InitDataVerifier verifier, MiniAppStore store, AdminIds admins,
                             AppUserRepository users, AddressGeocoder geocoder, Screens screens) {
        this.verifier = verifier;
        this.store = store;
        this.admins = admins;
        this.users = users;
        this.geocoder = geocoder;
        this.screens = screens;
    }

    /** Новая точка из профиля: адрес текстом или координаты с телефона. */
    public record OriginRequest(String address, Double lat, Double lon) {
    }

    /** Сохранённая точка — её имя показывается в профиле. */
    public record OriginResponse(String name, double lat, double lon) {
    }

    private static final double MAX_ORIGIN_KM = 100;
    private static final Pattern LETTER = Pattern.compile("\\p{L}");

    /**
     * Смена точки из профиля мини-приложения — то же, что /address в боте:
     * сохраняется в app_user, и следующая кнопка бота понесёт новую точку.
     * Правила те же: дальше 100 км от обоих городов адрес не принимаем, а
     * геолокацию оттуда заменяем центром ближайшего — иначе лента пуста.
     * 404 — адрес не нашёлся, 409 — анкета в боте не пройдена.
     */
    @PostMapping("/api/me/origin")
    public ResponseEntity<OriginResponse> origin(
            @RequestHeader(value = INIT_DATA, required = false) String initData,
            @RequestBody OriginRequest request) {
        OptionalLong userId = verifier.userId(initData);
        if (userId.isEmpty()) {
            log.debug("Смена точки без валидной подписи initData");
            return ResponseEntity.status(401).build();
        }
        AppUser user = users.findByMaxUserId(userId.getAsLong()).orElse(null);
        if (user == null || user.getDialogState() != DialogState.READY) {
            return ResponseEntity.status(409).build();
        }
        Screens.Origin point;
        if (request.lat() != null && request.lon() != null) {
            double lat = request.lat();
            double lon = request.lon();
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return ResponseEntity.badRequest().build();
            }
            Screens.Origin center = screens.nearestCenter(lat, lon);
            point = Haversine.distanceKm(lat, lon, center.lat(), center.lon()) > MAX_ORIGIN_KM
                    ? center
                    : new Screens.Origin(Screens.GEO_NAME, round4(lat), round4(lon));
        } else {
            String address = request.address() == null ? "" : request.address().trim();
            if (!LETTER.matcher(address).find() || address.length() > 200) {
                return ResponseEntity.status(404).build();
            }
            Optional<AddressGeocoder.Place> place;
            try {
                place = geocoder.geocode(address);
            } catch (RuntimeException e) {
                // Адрес в лог не пишем: он может быть домашним.
                log.warn("Геокодер упал: {}", e.getClass().getSimpleName());
                place = Optional.empty();
            }
            // Целый город вместо улицы — 422: приложение просит уточнить адрес.
            if (place.isPresent() && place.get().coarse()) {
                return ResponseEntity.status(422).build();
            }
            Optional<Screens.Origin> found = place
                    .filter(p -> {
                        Screens.Origin c = screens.nearestCenter(p.lat(), p.lon());
                        return Haversine.distanceKm(p.lat(), p.lon(), c.lat(), c.lon()) <= MAX_ORIGIN_KM;
                    })
                    .map(p -> new Screens.Origin(p.name(), round4(p.lat()), round4(p.lon())));
            if (found.isEmpty()) {
                return ResponseEntity.status(404).build();
            }
            point = found.get();
        }
        user.setOrigin(point.name(), point.lat(), point.lon());
        users.save(user);
        return ResponseEntity.ok(new OriginResponse(point.name(), point.lat(), point.lon()));
    }

    private static double round4(double value) {
        return Math.round(value * 10000) / 10000.0;
    }

    /** Прошёл ли человек анкету в боте: без неё у приложения нет ни баланса, ни точки. */
    public record Me(boolean registered) {
    }

    @GetMapping("/api/me")
    public ResponseEntity<Me> me(
            @RequestHeader(value = INIT_DATA, required = false) String initData) {
        OptionalLong userId = verifier.userId(initData);
        if (userId.isEmpty()) {
            log.debug("Запрос профиля без валидной подписи initData");
            return ResponseEntity.status(401).build();
        }
        boolean registered = users.findByMaxUserId(userId.getAsLong())
                .map(u -> u.getDialogState() == DialogState.READY)
                .orElse(false);
        return ResponseEntity.ok(new Me(registered));
    }

    /**
     * Тело действия. Невалидное значение бросает исключение из конструктора —
     * Jackson его оборачивает, и Spring отвечает 400.
     */
    public record ActionRequest(int eventId, MiniAppAction action, int priceRub, String category,
                                String city, OffsetDateTime startsAt, String title) {

        /** Старый клиент без даты и названия: напоминаний для таких действий не будет. */
        public ActionRequest(int eventId, MiniAppAction action, int priceRub, String category, String city) {
            this(eventId, action, priceRub, category, city, null, null);
        }

        private static final Pattern CATEGORY = Pattern.compile("^[A-Z_]{1,32}$");
        private static final Set<String> CITIES = Set.of("moscow", "kazan");

        public ActionRequest {
            if (eventId <= 0) {
                throw new IllegalArgumentException("eventId должен быть положительным");
            }
            if (action == null) {
                throw new IllegalArgumentException("не указано действие");
            }
            if (priceRub < 0 || priceRub > 100_000) {
                throw new IllegalArgumentException("цена вне диапазона 0..100000");
            }
            if (category != null && !CATEGORY.matcher(category).matches()) {
                throw new IllegalArgumentException("неизвестный формат категории");
            }
            if (city != null && !CITIES.contains(city)) {
                throw new IllegalArgumentException("неизвестный город");
            }
            if (title != null && title.length() > 300) {
                throw new IllegalArgumentException("название длиннее 300 символов");
            }
        }
    }

    @GetMapping("/api/me/reactions")
    public ResponseEntity<List<MiniAppStore.Reaction>> reactions(
            @RequestHeader(value = INIT_DATA, required = false) String initData) {
        OptionalLong userId = verifier.userId(initData);
        if (userId.isEmpty()) {
            log.debug("Запрос реакций без валидной подписи initData");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(store.reactions(userId.getAsLong()));
    }

    @PostMapping("/api/actions")
    public ResponseEntity<Void> action(
            @RequestHeader(value = INIT_DATA, required = false) String initData,
            @RequestBody ActionRequest request) {
        OptionalLong userId = verifier.userId(initData);
        if (userId.isEmpty()) {
            log.debug("Действие без валидной подписи initData");
            return ResponseEntity.status(401).build();
        }
        store.record(userId.getAsLong(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/stats")
    public ResponseEntity<MiniAppStats> stats(
            @RequestHeader(value = INIT_DATA, required = false) String initData) {
        OptionalLong userId = verifier.userId(initData);
        if (userId.isEmpty()) {
            log.debug("Статистика без валидной подписи initData");
            return ResponseEntity.status(401).build();
        }
        if (!admins.canSeeStats(userId.getAsLong())) {
            log.debug("Статистику запросил не администратор");
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(store.stats());
    }
}
