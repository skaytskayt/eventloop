package com.maxbot.eventLoop.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxUploadAPI;
import ru.max.botapi.model.ImageUploadedInfo;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;

/**
 * Картинка к приветствию.
 *
 * <p>Ссылкой её не отдать: MAX скачивает изображение со своей стороны, а
 * публичного адреса у приложения нет. Поэтому файл заливается в MAX и дальше
 * подставляется по токену.
 *
 * <p>Заливка ленивая и однократная: делать её на старте значит задерживать
 * запуск ради картинки и падать, когда сеть недоступна. Токен живёт в памяти —
 * после рестарта загрузим заново, это один запрос.
 */
@Component
public class WelcomeImage {

    private static final Logger log = LoggerFactory.getLogger(WelcomeImage.class);
    private static final String RESOURCE = "bot/welcome.png";

    private final MaxBotAPI api;
    private final AtomicReference<Map<String, PhotoAttachmentRequestPayload.TokenRef>> photos =
            new AtomicReference<>();

    public WelcomeImage(MaxBotAPI api) {
        this.api = api;
    }

    /**
     * Токены картинки или {@code null}, если залить не удалось. Приветствие
     * должно уйти и без неё: текст важнее оформления.
     */
    public Map<String, PhotoAttachmentRequestPayload.TokenRef> photos() {
        Map<String, PhotoAttachmentRequestPayload.TokenRef> cached = photos.get();
        if (cached != null) {
            return cached;
        }
        Map<String, PhotoAttachmentRequestPayload.TokenRef> uploaded = upload();
        if (uploaded != null) {
            photos.set(uploaded);
        }
        return uploaded;
    }

    private Map<String, PhotoAttachmentRequestPayload.TokenRef> upload() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream();
             MaxUploadAPI uploads = new MaxUploadAPI()) {
            byte[] bytes = in.readAllBytes();
            UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();
            ImageUploadedInfo info = uploads.uploadImage(endpoint, bytes, "welcome.png");
            log.info("Картинка приветствия загружена в MAX ({} КБ)", bytes.length / 1024);
            return info.photos();
        } catch (IOException e) {
            log.error("Не нашёл файл {} в ресурсах", RESOURCE, e);
            return null;
        } catch (RuntimeException e) {
            log.warn("Не удалось загрузить картинку приветствия — отправлю без неё: {}", e.getMessage());
            return null;
        }
    }
}
