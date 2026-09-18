package com.maxbot.eventLoop.dialog;

import com.maxbot.eventLoop.config.AppProperties;
import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.domain.DialogState;
import com.maxbot.eventLoop.gateway.MaxGateway;
import com.maxbot.eventLoop.gateway.WelcomeImage;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.Update;

/**
 * Весь диалог бота: два вопроса про деньги и ссылка на мини-приложение.
 *
 * <p>Кошельков два, поэтому и вопроса два. Одной суммой обойтись нельзя: где
 * именно потрачено недостающее, знает только держатель карты, и приложению
 * пришлось бы делить остаток наугад.
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

    private final AppUserRepository users;
    private final Screens screens;
    private final MaxGateway max;
    private final WelcomeImage welcomeImage;
    private final AppProperties properties;

    public DialogRouter(AppUserRepository users, Screens screens, MaxGateway max,
                        WelcomeImage welcomeImage, AppProperties properties) {
        this.users = users;
        this.screens = screens;
        this.max = max;
        this.welcomeImage = welcomeImage;
        this.properties = properties;
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

        if (text.toLowerCase(Locale.ROOT).startsWith("/start")) {
            restart(user, chatId);
            return;
        }

        switch (user.getDialogState()) {
            case NEW, AWAITING_CINEMA -> onCinemaInput(user, chatId, text);
            case AWAITING_OTHER -> onOtherInput(user, chatId, text);
            // Обе суммы названы. Любой текст после этого — повод показать
            // ссылку ещё раз, а не отчитывать за неправильную команду.
            case READY -> sendMiniApp(user, chatId);
        }
    }

    private void restart(AppUser user, long chatId) {
        user.setDialogState(DialogState.AWAITING_CINEMA);
        users.save(user);
        // Картинка только в приветствии: дальше идёт разговор про числа,
        // и иллюстрация к каждому ответу превращается в шум.
        max.sendCard(chatId, screens.welcome(), welcomeImage.photos(), null);
    }

    private void onCinemaInput(AppUser user, long chatId, String text) {
        switch (BalanceParser.parse(text, properties.cinemaLimitRub())) {
            case BalanceParser.Result.Error error -> max.sendText(chatId, error.message(), null);
            case BalanceParser.Result.Ok ok -> {
                user.setCinemaRub(ok.rubles());
                user.setDialogState(DialogState.AWAITING_OTHER);
                users.save(user);
                max.sendText(chatId, screens.askOther(ok.rubles()), null);
            }
        }
    }

    private void onOtherInput(AppUser user, long chatId, String text) {
        switch (BalanceParser.parse(text, properties.otherLimitRub())) {
            case BalanceParser.Result.Error error -> max.sendText(chatId, error.message(), null);
            case BalanceParser.Result.Ok ok -> {
                user.setOtherRub(ok.rubles());
                user.setDialogState(DialogState.READY);
                users.save(user);
                sendMiniApp(user, chatId);
            }
        }
    }

    /**
     * Единственный экран после обоих вопросов. Если адрес приложения не задан,
     * человек всё равно получает ответ, а не тишину — и видно, что чинить.
     */
    private void sendMiniApp(AppUser user, long chatId) {
        int cinema = user.getCinemaRub();
        int other = user.getOtherRub();
        var keyboard = screens.miniAppKeyboard(cinema, other);
        String text = screens.ready(cinema, other);
        if (keyboard == null) {
            log.warn("Адрес мини-приложения не задан (eventloop.miniapp-url) — отправляю без кнопки");
            text = text + screens.miniAppMissing();
        }
        max.sendText(chatId, text, keyboard);
    }

    private AppUser user(long maxUserId, long chatId) {
        AppUser user = users.findByMaxUserId(maxUserId).orElseGet(() -> new AppUser(maxUserId, chatId));
        if (user.getChatId() == null || user.getChatId() != chatId) {
            user.setChatId(chatId);
        }
        return users.save(user);
    }
}
