package com.maxbot.eventLoop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки бота.
 *
 * <p>Номиналы кошельков меняются приказами Минкультуры — держим в конфигурации,
 * а не в коде. Username бота, чьё мини-приложение открывает кнопка, приходит
 * переменной окружения: бот у разработки и у демо может быть разный.
 */
@ConfigurationProperties(prefix = "eventloop")
public record AppProperties(
        int cinemaLimitRub,
        int otherLimitRub,
        String botUsername
) {

    public AppProperties {
        if (cinemaLimitRub <= 0 || otherLimitRub <= 0) {
            throw new IllegalArgumentException(
                    "Номиналы кошельков должны быть положительными, получено: кино "
                            + cinemaLimitRub + ", остальное " + otherLimitRub);
        }
    }

    /** Номинал карты целиком — сумма двух кошельков. */
    public int nominalRub() {
        return cinemaLimitRub + otherLimitRub;
    }
}
