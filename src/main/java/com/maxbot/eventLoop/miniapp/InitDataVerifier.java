package com.maxbot.eventLoop.miniapp;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Проверка подписи {@code WebApp.initData} из Mini App MAX.
 *
 * <p>Только так сервер узнаёт, кто прислал действие: id пользователя в теле
 * запроса подделать может любой, а подпись токеном бота — нет.
 *
 * <p>Свежесть {@code auth_date} сознательно не проверяется: на демо приложение
 * может висеть открытым долго, и старый initData не должен внезапно превращаться
 * в 401 посреди защиты.
 */
@Component
public class InitDataVerifier {

    private static final String HMAC = "HmacSHA256";

    private final ObjectMapper mapper = new ObjectMapper();
    private final String botToken;

    public InitDataVerifier(@Value("${max.bot.longpolling.token:}") String botToken) {
        this.botToken = botToken;
    }

    /** Id пользователя MAX из корректно подписанной строки, иначе пусто. */
    public OptionalLong userId(String initData) {
        // Без токена подпись проверялась бы пустым ключом — такую подделает кто угодно.
        if (botToken == null || botToken.isBlank() || initData == null || initData.isEmpty()) {
            return OptionalLong.empty();
        }
        try {
            TreeMap<String, String> params = new TreeMap<>();
            for (String pair : initData.split("&")) {
                int eq = pair.indexOf('=');
                if (eq < 0) {
                    return OptionalLong.empty();
                }
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                if (params.put(key, value) != null) {
                    return OptionalLong.empty();
                }
            }
            String hash = params.remove("hash");
            if (hash == null) {
                return OptionalLong.empty();
            }
            String check = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));
            byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken);
            byte[] expected = HexFormat.of().formatHex(hmac(secret, check))
                    .getBytes(StandardCharsets.US_ASCII);
            byte[] actual = hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
            if (!MessageDigest.isEqual(expected, actual)) {
                return OptionalLong.empty();
            }
            String user = params.get("user");
            if (user == null) {
                return OptionalLong.empty();
            }
            JsonNode id = mapper.readTree(user).get("id");
            if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(id.asLong());
        } catch (Exception e) {
            // Кривой initData — это отказ в доступе, а не ошибка сервера.
            return OptionalLong.empty();
        }
    }

    private static byte[] hmac(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC);
        mac.init(new SecretKeySpec(key, HMAC));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
