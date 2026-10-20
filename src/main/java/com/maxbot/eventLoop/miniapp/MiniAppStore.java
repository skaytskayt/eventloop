package com.maxbot.eventLoop.miniapp;

import com.maxbot.eventLoop.miniapp.MiniAppController.ActionRequest;
import com.maxbot.eventLoop.miniapp.MiniAppStats.CategoryCount;
import com.maxbot.eventLoop.miniapp.MiniAppStats.RecentAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                        INSERT INTO miniapp_action (max_user_id, event_id, action, price_rub, category)
                        VALUES (?, ?, ?, ?, ?)
                        """)
                .params(maxUserId, request.eventId(), request.action().name(),
                        request.priceRub(), request.category())
                .update();
        MiniAppAction action = request.action();
        if (action.reactionKind() != null) {
            jdbc.sql("""
                            INSERT INTO miniapp_reaction (max_user_id, event_id, kind, price_rub, category)
                            VALUES (?, ?, ?, ?, ?)
                            ON CONFLICT (max_user_id, event_id) DO UPDATE
                            SET kind = EXCLUDED.kind,
                                price_rub = EXCLUDED.price_rub,
                                category = EXCLUDED.category,
                                updated_at = now()
                            """)
                    .params(maxUserId, request.eventId(), action.reactionKind(),
                            request.priceRub(), request.category())
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

    public MiniAppStats stats() {
        long users = jdbc.sql("SELECT count(DISTINCT max_user_id) FROM miniapp_action")
                .query(Long.class).single();

        // Каждый шаг воронки — уникальные пары «пользователь — событие»: десять
        // открытий одной карточки не должны раздувать «Увидели карточку».
        Map<String, Long> steps = new HashMap<>();
        jdbc.sql("""
                        SELECT action, count(*) AS n
                        FROM (SELECT DISTINCT action, max_user_id, event_id
                              FROM miniapp_action
                              WHERE action IN ('VIEW', 'LIKE', 'CLICK_BUY', 'PURCHASE_YES')) t
                        GROUP BY action
                        """)
                .query(rs -> {
                    steps.put(rs.getString("action"), rs.getLong("n"));
                });
        long view = steps.getOrDefault("VIEW", 0L);
        long like = steps.getOrDefault("LIKE", 0L);
        long clickBuy = steps.getOrDefault("CLICK_BUY", 0L);
        long purchaseYes = steps.getOrDefault("PURCHASE_YES", 0L);

        // Текущее состояние уже учитывает отменённые покупки — это и есть
        // «подтверждённые минус отменённые».
        long[] bought = jdbc.sql("""
                        SELECT COALESCE(SUM(price_rub), 0) AS rub, count(*) AS n
                        FROM miniapp_reaction
                        WHERE kind = 'BOUGHT'
                        """)
                .query((rs, n) -> new long[] {rs.getLong("rub"), rs.getLong("n")})
                .single();

        List<CategoryCount> likes = jdbc.sql("""
                        SELECT COALESCE(category, 'OTHER') AS category, count(*) AS n
                        FROM (SELECT DISTINCT max_user_id, event_id, category
                              FROM miniapp_action
                              WHERE action = 'LIKE') t
                        GROUP BY COALESCE(category, 'OTHER')
                        ORDER BY n DESC, category
                        """)
                .query((rs, n) -> new CategoryCount(rs.getString("category"), rs.getLong("n")))
                .list();

        List<RecentAction> recent = jdbc.sql("""
                        SELECT event_id, action, category, price_rub, created_at
                        FROM miniapp_action
                        ORDER BY id DESC
                        LIMIT 10
                        """)
                .query((rs, n) -> new RecentAction(
                        rs.getInt("event_id"),
                        rs.getString("action"),
                        rs.getString("category"),
                        rs.getInt("price_rub"),
                        rs.getTimestamp("created_at").getTime()))
                .list();

        return new MiniAppStats(users, MiniAppStats.funnel(view, like, clickBuy, purchaseYes),
                MiniAppStats.overallPercent(view, purchaseYes), bought[0], bought[1], likes, recent);
    }
}
