package com.maxbot.eventLoop.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.max.botapi.client.MaxApiException;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ImageAttachmentRequest;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.max.botapi.model.TextFormat;

/**
 * Единственное место, где живут типы SDK. Всё остальное приложение о нём не знает —
 * это страховка от ломающих изменений в линии 0.x.
 *
 * <p>MAX отвечает HTTP 200 и на неуспешные операции, поместив ошибку в тело.
 * Код, который смотрит только на статус, теряет все ошибки молча.
 *
 * <p>У домена есть лимит 30 rps и 2 операции в секунду на чат. Бот шлёт одно
 * сообщение на реплику, но придержать частоту дешевле, чем ловить 429.
 */
@Component
public class MaxGateway {

    private static final Logger log = LoggerFactory.getLogger(MaxGateway.class);

    /** Минимальный интервал между операциями в одном чате. */
    private static final long MIN_INTERVAL_MS = 350;

    private final MaxBotAPI api;
    private final Map<Long, Long> lastSentAt = new ConcurrentHashMap<>();

    public MaxGateway(MaxBotAPI api) {
        this.api = api;
    }

    /** Отправляет сообщение, при необходимости с клавиатурой под ним. */
    public void sendText(long chatId, String text, List<List<Button>> keyboard) {
        send(chatId, text, null, keyboard);
    }

    /**
     * То же с картинкой сверху. Токены берутся из загруженного в MAX файла:
     * ссылкой картинку не отдать, публичного адреса у приложения нет.
     *
     * <p>Если MAX не принял вложение, сообщение уходит без него: текст важнее
     * оформления, а «кнопка не работает» из-за одной картинки — худший исход.
     */
    public void sendCard(long chatId, String text,
                         Map<String, PhotoAttachmentRequestPayload.TokenRef> photos,
                         List<List<Button>> keyboard) {
        send(chatId, text, photos, keyboard);
    }

    private void send(long chatId, String text,
                      Map<String, PhotoAttachmentRequestPayload.TokenRef> photos,
                      List<List<Button>> keyboard) {
        throttle(chatId);
        try {
            api.sendMessage(body(text, photos, keyboard)).chatId(chatId).execute();
        } catch (MaxApiException e) {
            if (photos != null && isImageFailure(e)) {
                log.warn("MAX не принял картинку ({}) — отправляю без неё", e.errorMessage());
                send(chatId, text, null, keyboard);
                return;
            }
            log.error("Не удалось отправить сообщение в чат {}: HTTP {} code={} message={}",
                    chatId, e.statusCode(), e.errorCode(), e.errorMessage());
        } catch (RuntimeException e) {
            log.error("Сбой отправки сообщения в чат {}", chatId, e);
        }
    }

    private static boolean isImageFailure(MaxApiException e) {
        return e.statusCode() == 400
                && e.errorMessage() != null
                && e.errorMessage().toLowerCase().contains("image");
    }

    private NewMessageBody body(String text,
                                Map<String, PhotoAttachmentRequestPayload.TokenRef> photos,
                                List<List<Button>> keyboard) {
        List<AttachmentRequest> attachments = new ArrayList<>(2);
        if (photos != null && !photos.isEmpty()) {
            attachments.add(new ImageAttachmentRequest(
                    new PhotoAttachmentRequestPayload(null, null, photos)));
        }
        if (keyboard != null && !keyboard.isEmpty()) {
            attachments.add(new InlineKeyboardAttachmentRequest(
                    new InlineKeyboardAttachment.KeyboardPayload(keyboard)));
        }
        return new NewMessageBody(text, attachments.isEmpty() ? null : attachments,
                null, null, TextFormat.MARKDOWN);
    }

    /**
     * Придерживает частоту обращений по каждому чату. Вызывается из потока обработки
     * апдейта: при долгом поллинге апдейты приходят последовательно, поэтому ожидание
     * здесь сохраняет порядок сообщений — в обмен на задержку при быстрых нажатиях.
     */
    private void throttle(long chatId) {
        long now = System.currentTimeMillis();
        Long previous = lastSentAt.put(chatId, now);
        if (previous == null) {
            return;
        }
        long waitMs = MIN_INTERVAL_MS - (now - previous);
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
                lastSentAt.put(chatId, System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
