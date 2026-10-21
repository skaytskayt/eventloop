package com.maxbot.eventLoop.config;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Администраторы — пользователи MAX, которым видна статистика: команда /stats
 * в боте и экран статистики в Mini App.
 *
 * <p>Список берётся из {@code eventloop.admin-max-ids} (переменная ADMIN_MAX_IDS,
 * MAX user id через запятую). Пустой список — статистику не видит никто: отказ
 * по умолчанию, а не «все администраторы». Значения, которые не являются
 * положительным числом, пропускаются и доступа не дают.
 */
@Component
public class AdminIds {

    private static final Logger log = LoggerFactory.getLogger(AdminIds.class);

    private final Set<Long> ids;

    public AdminIds(@Value("${eventloop.admin-max-ids:}") String raw) {
        Set<Long> parsed = new HashSet<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String s = part.trim();
                if (s.isEmpty()) {
                    continue;
                }
                try {
                    long id = Long.parseLong(s);
                    if (id > 0) {
                        parsed.add(id);
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                    // ниже — предупреждение
                }
                log.warn("ADMIN_MAX_IDS: пропущено «{}» — это не id пользователя MAX", s);
            }
        }
        this.ids = Set.copyOf(parsed);
    }

    public boolean contains(long maxUserId) {
        return ids.contains(maxUserId);
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }
}
