package com.maxbot.eventLoop.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Живая проверка ссылок датасета.
 *
 * <p>Вынесена за пределы обычного прогона: она требует сети и может падать
 * по причинам, к коду отношения не имеющим. Запуск:
 *
 * <pre>./mvnw test -Dlinkcheck=true -Dtest=SeedLinkCheckTest</pre>
 *
 * <p>Прогонять перед демо — битая ссылка на билет ломает ровно тот момент,
 * ради которого продукт существует.
 */
@EnabledIfSystemProperty(named = "linkcheck", matches = "true")
class SeedLinkCheckTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Test
    @DisplayName("ссылки на мероприятия и билеты отвечают")
    void linksRespond() throws IOException {
        String sql = Files.readString(
                Path.of("src", "main", "resources", "db", "migration", "R__seed_events.sql"),
                StandardCharsets.UTF_8);

        TreeSet<String> urls = new TreeSet<>();
        Matcher m = Pattern.compile("'(https://[^']+)'").matcher(sql);
        while (m.find()) {
            urls.add(m.group(1));
        }
        assertThat(urls).isNotEmpty();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        Map<String, String> broken = new LinkedHashMap<>();
        List<String> checked = new ArrayList<>();

        for (String url : urls) {
            try {
                HttpResponse<Void> response = client.send(
                        HttpRequest.newBuilder(URI.create(url))
                                .timeout(TIMEOUT)
                                .header("User-Agent", "Mozilla/5.0 (compatible; EventLoopSeedCheck/1.0)")
                                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.discarding());
                checked.add(url);
                if (response.statusCode() >= 400) {
                    broken.put(url, "HTTP " + response.statusCode());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                broken.put(url, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        assertThat(broken)
                .as("проверено ссылок: %d. Недоступные:%n%s", checked.size(),
                        broken.entrySet().stream()
                                .map(en -> "  " + en.getKey() + " → " + en.getValue())
                                .reduce((a, b) -> a + "\n" + b).orElse(""))
                .isEmpty();
    }
}
