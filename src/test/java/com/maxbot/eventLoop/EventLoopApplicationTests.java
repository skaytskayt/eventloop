package com.maxbot.eventLoop;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Полный контекст поднять без Postgres и токена бота нельзя, поэтому здесь
 * проверяется то, что реально ломается тихо: разбор настроек из application.yml.
 *
 * <p>Сквозная проверка живого запуска — это {@code docker compose up},
 * она описана в README и в docs/max-api-facts.md.
 */
class EventLoopApplicationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfiguration.class);

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    static class PropertiesConfiguration {
    }

    @Test
    @DisplayName("номиналы кошельков читаются из конфигурации")
    void bindsCardProperties() {
        runner.withPropertyValues(
                        "eventloop.cinema-limit-rub=2000",
                        "eventloop.other-limit-rub=3000",
                        "eventloop.miniapp-url=https://example.test/app/")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AppProperties props = context.getBean(AppProperties.class);
                    assertThat(props.cinemaLimitRub()).isEqualTo(2000);
                    assertThat(props.otherLimitRub()).isEqualTo(3000);
                    assertThat(props.nominalRub()).isEqualTo(5000);
                    assertThat(props.miniappUrl()).isEqualTo("https://example.test/app/");
                });
    }

    /** Адрес приложения не обязателен: бот должен подниматься и без него. */
    @Test
    @DisplayName("без адреса мини-приложения контекст поднимается")
    void miniAppUrlIsOptional() {
        runner.withPropertyValues(
                        "eventloop.cinema-limit-rub=2000",
                        "eventloop.other-limit-rub=3000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AppProperties.class).miniappUrl()).isNull();
                });
    }

    @Test
    @DisplayName("нулевой номинал кошелька не даёт приложению стартовать")
    void rejectsContradictoryConfiguration() {
        runner.withPropertyValues(
                        "eventloop.cinema-limit-rub=0",
                        "eventloop.other-limit-rub=3000")
                .run(context -> assertThat(context).hasFailed());
    }
}
