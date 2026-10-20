package com.maxbot.eventLoop.miniapp;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS для /api/**.
 *
 * <p>Кнопка бота может открывать копию приложения с GitHub Pages — это чужой
 * origin, и без CORS браузер не отдаст ей ответ. Когда приложение раздаёт сам
 * бэкенд по /app/, запросы идут на тот же origin и эта настройка не участвует.
 */
@Configuration
public class MiniAppCorsConfig implements WebMvcConfigurer {

    private final String[] origins;

    public MiniAppCorsConfig(@Value("${eventloop.miniapp-origins:}") String origins) {
        this.origins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "X-Init-Data");
    }
}
