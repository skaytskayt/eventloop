package com.maxbot.eventLoop.gateway;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.max.botapi.client.MaxApiException;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.BotCommand;
import ru.max.botapi.model.BotCommandsPatch;
import ru.max.botapi.model.BotInfo;

/**
 * Проверка связи при старте: кто мы для MAX и доходит ли запрос вообще.
 *
 * <p>Выполняется внутри контейнера намеренно. Успешный {@code GET /me} с хоста
 * ничего не доказывает: домену нужен корневой сертификат Минцифры, которого нет
 * ни в системном хранилище Windows, ни в образе JRE — его везёт с собой SDK.
 * Поэтому проверять связь нужно оттуда, где бот на самом деле работает.
 *
 * <p>Ошибка здесь не мешает приложению подняться: здоровье контейнера не должно
 * зависеть от доступности внешнего сервиса. Но в логе она видна сразу.
 */
@Component
public class StartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupCheck.class);

    private final MaxBotAPI api;

    public StartupCheck(MaxBotAPI api) {
        this.api = api;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            BotInfo me = api.getMyInfo().execute();
            log.info("Связь с MAX есть. Бот: «{}», username: @{}, id: {}",
                    me.name(), me.username(), me.userId());
            log.info("Открыть диалог: https://max.ru/{}", me.username());
            publishCommands();
        } catch (MaxApiException e) {
            if (e.statusCode() == 401) {
                log.error("MAX отклонил токен (HTTP 401, {}). Токен недействителен или отозван.",
                        e.errorCode());
            } else {
                log.error("MAX ответил ошибкой на GET /me: HTTP {} code={} message={}",
                        e.statusCode(), e.errorCode(), e.errorMessage());
            }
        } catch (RuntimeException e) {
            // Сюда попадёт и отказ TLS, если однажды SDK перестанет везти сертификаты.
            log.error("Не удалось связаться с MAX при старте", e);
        }
    }

    /**
     * Публикует список команд, чтобы они были видны в интерфейсе MAX,
     * а не только в тексте приветствия.
     */
    private void publishCommands() {
        // Команда одна: бот спрашивает баланс и отдаёт ссылку на приложение.
        List<BotCommand> commands = List.of(
                new BotCommand("start", "указать баланс и открыть подборку"));
        try {
            api.editMyCommands(new BotCommandsPatch(commands)).execute();
            log.info("Команды бота опубликованы: {}",
                    commands.stream().map(BotCommand::name).toList());
        } catch (RuntimeException e) {
            // Не критично: команды продублированы в тексте приветствия и в /help.
            log.warn("Не удалось опубликовать список команд: {}", e.getMessage());
        }
    }
}
