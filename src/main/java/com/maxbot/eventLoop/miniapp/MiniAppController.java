package com.maxbot.eventLoop.miniapp;

import com.maxbot.eventLoop.config.AdminIds;
import com.maxbot.eventLoop.domain.DialogState;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.util.List;
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

    public MiniAppController(InitDataVerifier verifier, MiniAppStore store, AdminIds admins,
                             AppUserRepository users) {
        this.verifier = verifier;
        this.store = store;
        this.admins = admins;
        this.users = users;
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
                                String city) {

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
        if (!admins.contains(userId.getAsLong())) {
            log.debug("Статистику запросил не администратор");
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(store.stats());
    }
}
