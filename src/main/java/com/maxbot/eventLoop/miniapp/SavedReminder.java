package com.maxbot.eventLoop.miniapp;

import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.gateway.MaxGateway;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Напоминает в чате бота об отложенном событии за трое суток до начала.
 *
 * <p>Писать можно только тому, у кого есть чат с ботом: Mini App открывают и из
 * меню, ни разу не написав боту, — таким напоминание не уходит. Неудачная
 * отправка не повторяется: лучше пропустить одно напоминание, чем засыпать чат.
 */
@Component
public class SavedReminder {

    private static final Logger log = LoggerFactory.getLogger(SavedReminder.class);

    static final Duration AHEAD = Duration.ofDays(3);
    // Москва и Казань в одном поясе — время события показываем по нему.
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMMM 'в' HH:mm", Locale.forLanguageTag("ru"));

    private final MiniAppStore store;
    private final AppUserRepository users;
    private final MaxGateway gateway;

    public SavedReminder(MiniAppStore store, AppUserRepository users, MaxGateway gateway) {
        this.store = store;
        this.users = users;
        this.gateway = gateway;
    }

    @Scheduled(fixedDelay = 10, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void remind() {
        for (MiniAppStore.Due due : store.dueReminders(AHEAD)) {
            Long chatId = users.findByMaxUserId(due.maxUserId()).map(AppUser::getChatId).orElse(null);
            if (chatId != null) {
                gateway.sendText(chatId, text(due.title(), due.startsAt()), null);
            } else {
                log.debug("Нет чата с пользователем {} — напоминание пропущено", due.maxUserId());
            }
            store.markReminded(due.maxUserId(), due.eventId());
        }
    }

    static String text(String title, Instant startsAt) {
        return "⏰ Через три дня — **" + escape(title) + "**, "
                + WHEN.format(startsAt.atZone(ZONE)) + ".\n"
                + "Оно у тебя в отложенных — самое время решить и купить билет.";
    }

    // Название из чужой афиши: звёздочка или подчёркивание сломали бы разметку.
    private static String escape(String s) {
        return s.replaceAll("([*_`\\[\\]\\\\])", "\\\\$1");
    }
}
