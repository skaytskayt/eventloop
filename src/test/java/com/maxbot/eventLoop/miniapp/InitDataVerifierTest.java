package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InitDataVerifierTest {

    private static final String TOKEN = "test-token";
    private static final String USER = "{\"id\":12345,\"first_name\":\"Аня\"}";

    private final InitDataVerifier verifier = new InitDataVerifier(TOKEN);

    /** Ключи нарочно не по алфавиту: подпись считается по отсортированным. */
    private static Map<String, String> params(String user) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("user", user);
        p.put("query_id", "AAF-42");
        p.put("auth_date", "1760950000");
        return p;
    }

    @Test
    @DisplayName("корректно подписанная строка даёт id пользователя")
    void validSignature() {
        assertThat(verifier.userId(sign(params(USER), TOKEN))).isEqualTo(OptionalLong.of(12345));
    }

    @Test
    @DisplayName("подменённый после подписи user отвергается")
    void tamperedUser() {
        String signed = sign(params(USER), TOKEN);
        String forged = signed.replace(enc(USER), enc("{\"id\":99999,\"first_name\":\"Аня\"}"));
        assertThat(forged).isNotEqualTo(signed);
        assertThat(verifier.userId(forged)).isEmpty();
    }

    @Test
    @DisplayName("без hash, null и пустая строка — пусто")
    void missingHash() {
        String signed = sign(params(USER), TOKEN);
        String withoutHash = signed.replaceAll("&?hash=[0-9a-f]+", "");
        assertThat(verifier.userId(withoutHash)).isEmpty();
        assertThat(verifier.userId(null)).isEmpty();
        assertThat(verifier.userId("")).isEmpty();
    }

    @Test
    @DisplayName("подпись чужим токеном не проходит")
    void otherToken() {
        assertThat(verifier.userId(sign(params(USER), "other-token"))).isEmpty();
    }

    @Test
    @DisplayName("верификатор без токена отвергает всё")
    void emptyToken() {
        assertThat(new InitDataVerifier("").userId(sign(params(USER), ""))).isEmpty();
        assertThat(new InitDataVerifier(null).userId(sign(params(USER), TOKEN))).isEmpty();
    }

    @Test
    @DisplayName("user без целого положительного id — пусто")
    void badUser() {
        Map<String, String> noUser = params(USER);
        noUser.remove("user");
        assertThat(verifier.userId(sign(noUser, TOKEN))).isEmpty();
        assertThat(verifier.userId(sign(params("не json"), TOKEN))).isEmpty();
        assertThat(verifier.userId(sign(params("{\"first_name\":\"Аня\"}"), TOKEN))).isEmpty();
        assertThat(verifier.userId(sign(params("{\"id\":\"12345\"}"), TOKEN))).isEmpty();
        assertThat(verifier.userId(sign(params("{\"id\":1.5}"), TOKEN))).isEmpty();
        assertThat(verifier.userId(sign(params("{\"id\":-3}"), TOKEN))).isEmpty();
    }

    @Test
    @DisplayName("повтор ключа — отказ")
    void duplicateKey() {
        String signed = sign(params(USER), TOKEN);
        assertThat(verifier.userId(signed + "&auth_date=1760950000")).isEmpty();
    }

    private static String sign(Map<String, String> params, String token) {
        String check = new TreeMap<>(params).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), token);
        String hash = HexFormat.of().formatHex(hmac(secret, check));
        String query = params.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
        return query + "&hash=" + hash;
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
