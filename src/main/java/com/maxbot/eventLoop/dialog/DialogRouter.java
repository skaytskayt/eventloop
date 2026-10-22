package com.maxbot.eventLoop.dialog;

import com.maxbot.eventLoop.config.AdminIds;
import com.maxbot.eventLoop.config.AppProperties;
import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.domain.DialogState;
import com.maxbot.eventLoop.gateway.MaxGateway;
import com.maxbot.eventLoop.gateway.WelcomeImage;
import com.maxbot.eventLoop.geo.AddressGeocoder;
import com.maxbot.eventLoop.geo.Haversine;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.LocationAttachment;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.Update;

/**
 * Весь диалог бота: три вопроса — два про деньги и точка отсчёта — и кнопка
 * мини-приложения.
 *
 * <p>Кошельков два, поэтому и вопроса про деньги два: сколько всего на карте и
 * сколько из этого на кино. Остальное — разница. Одной суммой обойтись нельзя:
 * где именно потрачено недостающее, знает только держатель карты, и приложению
 * пришлось бы делить остаток наугад.
 *
 * <p>Третий вопрос — откуда считать расстояние: геолокация или адрес текстом,
 * который ищется через Nominatim. Геолокацию умеет запросить только бот
 * (кнопка request_geo_location), поэтому точка выбирается здесь и уезжает в
 * мини-приложение вместе с суммами; по ней же выбирается город ленты.
 *
 * <p>Раньше здесь жила лента со свайпами, меню и фильтрами. Их забрало
 * мини-приложение: листать карточки в переписке медленнее, а две реализации
 * одной ленты расходятся на второй же правке.
 *
 * <p>Состояние диалога лежит в базе, а не в памяти: рестарт контейнера не
 * должен оставлять человека посреди разговора.
 */
@Component
public class DialogRouter implements UpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(DialogRouter.class);

    /** Дальше этого от обоих центров лента пуста при любом фильтре (максимум — 50 км). */
    private static final double MAX_ORIGIN_KM = 100;
    /** Длиннее адрес не бывает; простыни в геокодер не уходят. */
    private static final int MAX_ADDRESS_LENGTH = 200;
    private static final Pattern LETTER = Pattern.compile("\\p{L}");

    private final AppUserRepository users;
    private final Screens screens;
    private final MaxGateway max;
    private final WelcomeImage welcomeImage;
    private final AppProperties properties;
    private final AdminIds admins;
    private final AddressGeocoder geocoder;

    public DialogRouter(AppUserRepository users, Screens screens, MaxGateway max,
                        WelcomeImage welcomeImage, AppProperties properties, AdminIds admins,
                        AddressGeocoder geocoder) {
        this.users = users;
        this.screens = screens;
        this.max = max;
        this.welcomeImage = welcomeImage;
        this.properties = properties;
        this.admins = admins;
        this.geocoder = geocoder;
    }

    @Override
    public void onUpdate(Update update) {
        try {
            switch (update) {
                case BotStartedUpdate u -> onStart(u.user().userId(), u.chatId());
                case MessageCreatedUpdate u -> onText(u);
                // Кнопок с callback у бота больше нет, остальные типы апдейтов
                // нас не касаются.
                default -> log.debug("Апдейт {} пропущен", update.updateType());
            }
        } catch (RuntimeException e) {
            log.error("Ошибка обработки апдейта {}", update.updateType(), e);
        }
    }

    private void onStart(long maxUserId, long chatId) {
        restart(user(maxUserId, chatId), chatId);
    }

    private void onText(MessageCreatedUpdate update) {
        var body = update.message().body();
        var recipient = update.message().recipient();
        if (body == null || recipient == null || recipient.chatId() == null) {
            log.info("Сообщение отброшено: body={}, recipient={}", body, recipient);
            return;
        }
        var sender = update.message().sender();
        if (sender == null || sender.isBot()) {
            log.info("Сообщение отброшено: отправитель {}",
                    sender == null ? "отсутствует" : "бот");
            return;
        }

        long chatId = recipient.chatId();
        AppUser user = user(sender.userId(), chatId);
        String text = body.text() == null ? "" : body.text().trim();
        LocationAttachment location = body.attachments() == null ? null : body.attachments().stream()
                .filter(LocationAttachment.class::isInstance)
                .map(LocationAttachment.class::cast)
                .findFirst().orElse(null);

        if (text.toLowerCase(Locale.ROOT).startsWith("/start")) {
            restart(user, chatId);
            return;
        }
        if (text.toLowerCase(Locale.ROOT).startsWith("/stats")) {
            sendStats(sender.userId(), chatId);
            return;
        }

        switch (user.getDialogState()) {
            case NEW, AWAITING_TOTAL -> onTotalInput(user, chatId, text);
            case AWAITING_CINEMA -> onCinemaInput(user, chatId, text);
            case AWAITING_ORIGIN -> onOriginInput(user, chatId, text, location);
            // Всё названо. Любой текст после этого — повод показать кнопку ещё
            // раз, а не отчитывать за неправильную команду. Геолокация меняет
            // точку (кнопка из вопроса остаётся в истории чата). Текст здесь
            // не геокодируется: Nominatim находит даже «привет» — объект в
            // Казани, — и точка молча уехала бы. Адрес меняется через /start.
            case READY -> {
                if (applyLocation(user, location)) {
                    users.save(user);
                }
                sendMiniApp(user, chatId);
            }
        }
    }

    private void restart(AppUser user, long chatId) {
        user.setDialogState(DialogState.AWAITING_TOTAL);
        users.save(user);
        // Картинка только в приветствии: дальше идёт разговор про числа,
        // и иллюстрация к каждому ответу превращается в шум.
        max.sendCard(chatId, screens.welcome(), welcomeImage.photos(), null);
    }

    /**
     * Общая сумма. Пока про кино не спросили, вся она числится в «остальном»:
     * так её не нужно хранить отдельно, а следующий шаг переложит часть в кино.
     */
    private void onTotalInput(AppUser user, long chatId, String text) {
        switch (BalanceParser.parse(text, properties.nominalRub())) {
            case BalanceParser.Result.Error error -> max.sendText(chatId, error.message(), null);
            case BalanceParser.Result.Ok ok -> {
                user.setCinemaRub(0);
                user.setOtherRub(ok.rubles());
                user.setDialogState(DialogState.AWAITING_CINEMA);
                users.save(user);
                max.sendText(chatId, screens.askCinema(ok.rubles()), null);
            }
        }
    }

    /**
     * Часть общей суммы на кино. Остальное — разница, и она обязана влезть в
     * свой кошелёк: при 5000 ₽ на карте на кино не может быть 1000 ₽, иначе
     * на остальное вышло бы 4000 при номинале 3000.
     */
    private void onCinemaInput(AppUser user, long chatId, String text) {
        int total = user.getOtherRub();
        // Без предела у парсера: любую сумму сверх допустимой объясняет
        // cinemaOutOfRange, а не общее «больше не может быть».
        switch (BalanceParser.parse(text, Integer.MAX_VALUE)) {
            case BalanceParser.Result.Error error -> max.sendText(chatId, error.message(), null);
            case BalanceParser.Result.Ok ok when ok.rubles() < screens.minCinema(total)
                    || ok.rubles() > screens.maxCinema(total) ->
                    max.sendText(chatId, screens.cinemaOutOfRange(total), null);
            case BalanceParser.Result.Ok ok -> {
                user.setCinemaRub(ok.rubles());
                user.setOtherRub(total - ok.rubles());
                user.setDialogState(DialogState.AWAITING_ORIGIN);
                users.save(user);
                max.sendText(chatId, screens.askOrigin(), screens.originKeyboard());
            }
        }
    }

    /**
     * Точка отсчёта: геолокация или адрес текстом, который ищется через
     * Nominatim. Не нашёлся, дальше 100 км от обоих городов или геокодер
     * не ответил — говорим «не нашёл» и остаёмся на этом шаге.
     */
    private void onOriginInput(AppUser user, long chatId, String text, LocationAttachment location) {
        if (applyLocation(user, location)) {
            user.setDialogState(DialogState.READY);
            users.save(user);
            sendMiniApp(user, chatId);
            return;
        }
        if (text.isEmpty()) {
            // Стикер, фото или пустое сообщение — просто повторяем вопрос.
            max.sendText(chatId, screens.askOrigin(), screens.originKeyboard());
            return;
        }
        // Текст без букв (обычно сумма, присланная не туда) и простыни в
        // геокодер не отправляем: ни то ни другое адресом не будет.
        if (!LETTER.matcher(text).find() || text.length() > MAX_ADDRESS_LENGTH) {
            max.sendText(chatId, screens.addressNotFound(), screens.originKeyboard());
            return;
        }
        Optional<AddressGeocoder.Place> place = lookup(text);
        if (place.isEmpty()) {
            max.sendText(chatId, screens.addressNotFound(), screens.originKeyboard());
            return;
        }
        AddressGeocoder.Place found = place.get();
        user.setOrigin(found.name(), round4(found.lat()), round4(found.lon()));
        user.setDialogState(DialogState.READY);
        users.save(user);
        max.sendText(chatId, screens.addressFound(found.name()), null);
        sendMiniApp(user, chatId);
    }

    /** Место по адресу; пусто — не найдено, сбой или дальше {@link #MAX_ORIGIN_KM} от обоих городов. */
    private Optional<AddressGeocoder.Place> lookup(String text) {
        Optional<AddressGeocoder.Place> place;
        try {
            place = geocoder.geocode(text);
        } catch (RuntimeException e) {
            // Текст адреса в лог не пишем: он может быть домашним.
            log.warn("Геокодер упал: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
        return place.filter(p -> {
            Screens.Origin center = screens.nearestCenter(p.lat(), p.lon());
            return Haversine.distanceKm(p.lat(), p.lon(), center.lat(), center.lon()) <= MAX_ORIGIN_KM;
        });
    }

    /**
     * Геолокация из кнопки request_geo_location; false — её нет или координаты
     * невозможные. Координаты округляются до 4 знаков (~11 м): точнее для
     * «до N км» не нужно, а лишняя точность — лишние сведения о человеке.
     */
    private boolean applyLocation(AppUser user, LocationAttachment location) {
        if (location == null || location.latitude() < -90 || location.latitude() > 90
                || location.longitude() < -180 || location.longitude() > 180) {
            return false;
        }
        double lat = location.latitude();
        double lon = location.longitude();
        Screens.Origin center = screens.nearestCenter(lat, lon);
        // Дальше 100 км от обоих городов лента пуста при любом фильтре
        // (максимум — 50 км), поэтому считаем от центра ближайшего.
        if (Haversine.distanceKm(lat, lon, center.lat(), center.lon()) > MAX_ORIGIN_KM) {
            user.setOrigin(center.name(), center.lat(), center.lon());
        } else {
            user.setOrigin(Screens.GEO_NAME, round4(lat), round4(lon));
        }
        return true;
    }

    private static double round4(double value) {
        return Math.round(value * 10000) / 10000.0;
    }

    /**
     * Единственный экран после обоих вопросов. Если username бота не задан,
     * человек всё равно получает ответ, а не тишину — и видно, что чинить.
     */
    private void sendMiniApp(AppUser user, long chatId) {
        int cinema = user.getCinemaRub();
        int other = user.getOtherRub();
        var keyboard = screens.miniAppKeyboard(cinema, other, user.getOriginLat(), user.getOriginLon());
        String text = screens.ready() + screens.originNote(user.getOriginName());
        if (keyboard == null) {
            log.warn("Username бота не задан (eventloop.bot-username) — отправляю без кнопки");
            text = text + screens.miniAppMissing();
        }
        max.sendText(chatId, text, keyboard);
    }

    /**
     * Скрытая админская команда: состояние диалога не меняет и в меню команд
     * бота не регистрируется. Не администратору отвечает, что такой команды
     * нет: ни о ролях, ни о самой статистике чужим знать незачем. Его id
     * остаётся только в логе сервера — оттуда его берут для ADMIN_MAX_IDS.
     */
    private void sendStats(long maxUserId, long chatId) {
        if (!admins.contains(maxUserId)) {
            log.info("/stats от не администратора, MAX id {}", maxUserId);
            max.sendText(chatId, screens.unknownCommand(), null);
            return;
        }
        var keyboard = screens.statsKeyboard();
        if (keyboard == null) {
            log.warn("Username бота не задан (eventloop.bot-username) — статистику отправляю без кнопки");
            max.sendText(chatId, screens.statsTitle() + screens.statsMissing(), null);
            return;
        }
        max.sendText(chatId, screens.statsTitle(), keyboard);
    }

    private AppUser user(long maxUserId, long chatId) {
        AppUser user = users.findByMaxUserId(maxUserId).orElseGet(() -> new AppUser(maxUserId, chatId));
        if (user.getChatId() == null || user.getChatId() != chatId) {
            user.setChatId(chatId);
        }
        return users.save(user);
    }
}
