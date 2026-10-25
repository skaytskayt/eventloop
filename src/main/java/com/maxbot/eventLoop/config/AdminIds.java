package com.maxbot.eventLoop.config;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 *
 * <p>{@code eventloop.stats-public} (STATS_PUBLIC) открывает статистику всем —
 * только для показа экспертам, что у проекта есть аналитика. В проде он
 * выключен, и статистика остаётся у администраторов.
 */
@Component
public class AdminIds {

    private static final Logger log = LoggerFactory.getLogger(AdminIds.class);

    private final Set<Long> ids;
    private final boolean statsPublic;

    public AdminIds(String raw) {
        this(raw, false);
    }

    @Autowired
    public AdminIds(@Value("${eventloop.admin-max-ids:}") String raw,
                    @Value("${eventloop.stats-public:false}") boolean statsPublic) {
        this.statsPublic = statsPublic;
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

    /** Видна ли статистика этому пользователю: администратору или всем в демо-режиме. */
    public boolean canSeeStats(long maxUserId) {
        return statsPublic || contains(maxUserId);
    }

    /** Демо-режим: статистика открыта всем. */
    public boolean statsPublic() {
        return statsPublic;
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }
}
