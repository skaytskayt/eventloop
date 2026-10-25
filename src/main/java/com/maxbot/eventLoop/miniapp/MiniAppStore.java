package com.maxbot.eventLoop.miniapp;

import com.maxbot.eventLoop.miniapp.MiniAppController.ActionRequest;
import com.maxbot.eventLoop.miniapp.MiniAppStats.BoughtRow;
import com.maxbot.eventLoop.miniapp.MiniAppStats.LogRow;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реакции и журнал действий Mini App.
 *
 * <p>Цена и категория приходят от клиента и не сверяются: сервер не знает ленту
 * из app/data.js. Для хакатонной статистики это допустимо — подпись initData
 * всё равно не даёт писать от чужого имени.
 */
@Repository
public class MiniAppStore {

    private final JdbcClient jdbc;

    public MiniAppStore(DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
    }

    public record Reaction(int eventId, String kind, int priceRub, String category, long at) {
    }

    public List<Reaction> reactions(long maxUserId) {
        return jdbc.sql("""
                        SELECT event_id, kind, price_rub, category, updated_at
                        FROM miniapp_reaction
                        WHERE max_user_id = ?
                        ORDER BY updated_at, event_id
                        """)
                .param(maxUserId)
                .query((rs, n) -> new Reaction(
                        rs.getInt("event_id"),
                        rs.getString("kind"),
                        rs.getInt("price_rub"),
                        rs.getString("category"),
                        rs.getTimestamp("updated_at").getTime()))
                .list();
    }

    @Transactional
    public void record(long maxUserId, ActionRequest request) {
        jdbc.sql("""
                        INSERT INTO miniapp_action (max_user_id, event_id, action, price_rub, category, city)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)
                .params(maxUserId, request.eventId(), request.action().name(),
                        request.priceRub(), request.category(), request.city())
                .update();
        MiniAppAction action = request.action();
        if (action.reactionKind() != null) {
            jdbc.sql("""
                            INSERT INTO miniapp_reaction (max_user_id, event_id, kind, price_rub, category, city,
                                                          starts_at, title)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (max_user_id, event_id) DO UPDATE
                            SET kind = EXCLUDED.kind,
                                price_rub = EXCLUDED.price_rub,
                                category = EXCLUDED.category,
                                -- клиент старой версии без города не стирает известный
                                city = COALESCE(EXCLUDED.city, miniapp_reaction.city),
                                starts_at = COALESCE(EXCLUDED.starts_at, miniapp_reaction.starts_at),
                                title = COALESCE(EXCLUDED.title, miniapp_reaction.title),
                                -- отложил заново после «Пойду» — напоминание снова в силе
                                reminded_at = CASE WHEN miniapp_reaction.kind = EXCLUDED.kind
                                                   THEN miniapp_reaction.reminded_at END,
                                updated_at = now()
                            """)
                    .params(maxUserId, request.eventId(), action.reactionKind(),
                            request.priceRub(), request.category(), request.city(),
                            request.startsAt(), request.title())
                    .update();
        } else if (action.clearsReaction()) {
            // Undo в клиенте снимает только оценку: купленное остаётся купленным.
            jdbc.sql("""
                            DELETE FROM miniapp_reaction
                            WHERE max_user_id = ? AND event_id = ? AND kind <> 'BOUGHT'
                            """)
                    .params(maxUserId, request.eventId())
                    .update();
        }
    }

    public record Due(long maxUserId, int eventId, String title, Instant startsAt) {
    }

    /**
     * Отложенное, до начала которого осталось не больше {@code ahead}, а
     * напоминания ещё не было. Отложенное уже внутри этого окна не берётся:
     * человек только что видел карточку, напоминать ему не о чем.
     */
    public List<Due> dueReminders(Duration ahead) {
        Instant now = Instant.now();
        return jdbc.sql("""
                        SELECT max_user_id, event_id, title, starts_at
                        FROM miniapp_reaction
                        WHERE kind = 'SAVED' AND reminded_at IS NULL AND title IS NOT NULL
                          AND starts_at > ? AND starts_at <= ?
                          AND updated_at < starts_at - make_interval(secs => ?)
                        """)
                .params(Timestamp.from(now), Timestamp.from(now.plus(ahead)), (double) ahead.toSeconds())
                .query((rs, n) -> new Due(
                        rs.getLong("max_user_id"),
                        rs.getInt("event_id"),
                        rs.getString("title"),
                        rs.getTimestamp("starts_at").toInstant()))
                .list();
    }

    public void markReminded(long maxUserId, int eventId) {
        jdbc.sql("UPDATE miniapp_reaction SET reminded_at = now() WHERE max_user_id = ? AND event_id = ?")
                .params(maxUserId, eventId)
                .update();
    }

    // Журнал читается целиком сознательно: на объёмах пилота (сотни-тысячи строк)
    // это доли миллисекунды, а метрики остаются чистыми функциями с тестами.
    // Примерно со 100 тыс. строк агрегаты надо переносить в SQL.
    public MiniAppStats stats() {
        List<LogRow> log = jdbc.sql("""
                        SELECT max_user_id, event_id, action, price_rub, category, city, created_at
                        FROM miniapp_action
                        ORDER BY id
                        """)
                .query((rs, n) -> new LogRow(
                        rs.getLong("max_user_id"),
                        rs.getInt("event_id"),
                        rs.getString("action"),
                        rs.getInt("price_rub"),
                        rs.getString("category"),
                        rs.getString("city"),
                        rs.getTimestamp("created_at").toInstant()))
                .list();

        // Текущее состояние уже учитывает отменённые покупки.
        List<BoughtRow> bought = jdbc.sql("""
                        SELECT max_user_id, event_id, price_rub, category, city
                        FROM miniapp_reaction
                        WHERE kind = 'BOUGHT'
                        """)
                .query((rs, n) -> new BoughtRow(
                        rs.getLong("max_user_id"),
                        rs.getInt("event_id"),
                        rs.getInt("price_rub"),
                        rs.getString("category"),
                        rs.getString("city")))
                .list();

        MiniAppStats.BotUsers botUsers = jdbc.sql("""
                        SELECT count(*) AS total, count(*) FILTER (WHERE dialog_state = 'READY') AS ready
                        FROM app_user
                        """)
                .query((rs, n) -> new MiniAppStats.BotUsers(rs.getLong("total"), rs.getLong("ready")))
                .single();

        return MiniAppStats.compute(log, bought, botUsers, Instant.now());
    }
}
