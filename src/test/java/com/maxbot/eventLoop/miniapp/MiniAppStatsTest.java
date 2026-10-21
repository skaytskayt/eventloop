package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.miniapp.MiniAppStats.Activity;
import com.maxbot.eventLoop.miniapp.MiniAppStats.BoughtRow;
import com.maxbot.eventLoop.miniapp.MiniAppStats.CategoryStat;
import com.maxbot.eventLoop.miniapp.MiniAppStats.CityStat;
import com.maxbot.eventLoop.miniapp.MiniAppStats.FunnelStep;
import com.maxbot.eventLoop.miniapp.MiniAppStats.LogRow;
import com.maxbot.eventLoop.miniapp.MiniAppStats.Median;
import com.maxbot.eventLoop.miniapp.MiniAppStats.Money;
import com.maxbot.eventLoop.miniapp.MiniAppStats.Ratio;
import com.maxbot.eventLoop.miniapp.MiniAppStats.TopEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MiniAppStatsTest {

    /** 13:00 по Москве. */
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final ZoneId MSK = MiniAppStats.MOSCOW;

    private static LogRow row(long user, int event, String action) {
        return new LogRow(user, event, action, 500, "THEATRE", "moscow", NOW);
    }

    private static LogRow row(long user, int event, String action, Instant at) {
        return new LogRow(user, event, action, 500, "THEATRE", "moscow", at);
    }

    private static LogRow row(long user, int event, String action, String category, String city) {
        return new LogRow(user, event, action, 500, category, city, NOW);
    }

    private static BoughtRow buy(long user, int event, int price) {
        return new BoughtRow(user, event, price, "THEATRE", "moscow");
    }

    private static BoughtRow buy(long user, int event, int price, String city) {
        return new BoughtRow(user, event, price, "THEATRE", city);
    }

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }

    @Test
    @DisplayName("свайпов до выбора — медиана просмотров до первого «Пойду» по порядку журнала")
    void swipesToLike() {
        // Строки перемешаны между пользователями, время у всех одинаковое —
        // считается по позиции в списке (порядок id).
        List<LogRow> log = List.of(
                row(1, 1, "VIEW"), row(2, 1, "VIEW"), row(1, 2, "VIEW"),
                row(3, 1, "VIEW"), row(4, 1, "LIKE"), row(1, 3, "VIEW"),
                row(2, 1, "LIKE"), row(1, 4, "LIKE"), row(3, 2, "VIEW"),
                row(1, 5, "VIEW"), row(1, 5, "LIKE"));
        Median m = MiniAppStats.medianViewsBeforeFirstLike(log);
        assertThat(m.value()).isEqualTo(1.0);
        assertThat(m.users()).isEqualTo(3);

        List<LogRow> even = List.of(
                row(1, 1, "VIEW"), row(1, 2, "LIKE"),
                row(2, 1, "VIEW"), row(2, 2, "VIEW"), row(2, 3, "LIKE"));
        assertThat(MiniAppStats.medianViewsBeforeFirstLike(even)).isEqualTo(new Median(1.5, 2));

        List<LogRow> noLikes = List.of(row(1, 1, "VIEW"), row(2, 1, "DISLIKE"));
        assertThat(MiniAppStats.medianViewsBeforeFirstLike(noLikes)).isEqualTo(new Median(null, 0));
    }

    @Test
    @DisplayName("₽ доведено до покупки — сумма, количество и средний чек")
    void boughtTotals() {
        assertThat(MiniAppStats.boughtTotals(List.of(buy(1, 1, 1000), buy(2, 2, 500), buy(3, 3, 600))))
                .isEqualTo(new Money(2100, 3, 700L));
        assertThat(MiniAppStats.boughtTotals(List.of())).isEqualTo(new Money(0, 0, null));
    }

    @Test
    @DisplayName("из «Пойду» в покупку — доля уникальных отмеченных пар, которые сейчас куплены")
    void likedToBought() {
        List<LogRow> log = List.of(
                row(1, 10, "LIKE"), row(1, 11, "LIKE"), row(2, 10, "LIKE"), row(2, 10, "LIKE"));
        Ratio r = MiniAppStats.likedToBought(log, List.of(buy(1, 10, 500), buy(3, 12, 500)));
        assertThat(r).isEqualTo(new Ratio(1, 3, 33.3));
    }

    @Test
    @DisplayName("из «Пойду» в покупку без лайков — процента нет, а не деление на ноль")
    void likedToBoughtWithoutLikes() {
        Ratio r = MiniAppStats.likedToBought(List.of(row(1, 10, "VIEW")), List.of(buy(1, 10, 500)));
        assertThat(r).isEqualTo(new Ratio(0, 0, null));
    }

    @Test
    @DisplayName("активны — сегодня и за 7 дней по Москве, новых сегодня")
    void activeUsers() {
        List<LogRow> log = List.of(
                row(1, 1, "VIEW", at("2026-10-18T09:00:00Z")),
                row(4, 1, "VIEW", at("2026-10-14T12:00:00Z")),
                row(3, 1, "VIEW", at("2026-10-20T20:30:00Z")),
                row(2, 1, "VIEW", at("2026-10-20T21:30:00Z")),
                row(1, 2, "LIKE", at("2026-10-21T08:00:00Z")));
        assertThat(MiniAppStats.activeUsers(log, NOW, MSK)).isEqualTo(new Activity(2, 3, 1));
        assertThat(MiniAppStats.activeUsers(List.of(), NOW, MSK)).isEqualTo(new Activity(0, 0, 0));
    }

    @Test
    @DisplayName("воронка по людям — открыли, отметили, перешли к покупке, купили")
    void peopleFunnel() {
        List<LogRow> log = List.of(
                row(1, 1, "VIEW"), row(2, 1, "VIEW"), row(3, 1, "VIEW"), row(4, 1, "VIEW"),
                row(1, 1, "LIKE"), row(2, 1, "LIKE"), row(3, 1, "LIKE"), row(3, 2, "LIKE"),
                row(1, 1, "CLICK_BUY"), row(2, 1, "CLICK_BUY"), row(2, 1, "CLICK_BUY"),
                row(1, 1, "PURCHASE_YES"));
        List<FunnelStep> steps = MiniAppStats.peopleFunnel(log, List.of(buy(1, 1, 500)));
        assertThat(steps).containsExactly(
                new FunnelStep("OPENED", 4, 100.0),
                new FunnelStep("LIKED", 3, 75.0),
                new FunnelStep("CLICK_BUY", 2, 50.0),
                new FunnelStep("BOUGHT", 1, 25.0));
    }

    @Test
    @DisplayName("воронка без журнала — нули и пустые проценты, а не бесконечность")
    void peopleFunnelDivisionByZero() {
        assertThat(MiniAppStats.peopleFunnel(List.of(), List.of())).containsExactly(
                new FunnelStep("OPENED", 0, null),
                new FunnelStep("LIKED", 0, null),
                new FunnelStep("CLICK_BUY", 0, null),
                new FunnelStep("BOUGHT", 0, null));
        List<FunnelStep> orphan = MiniAppStats.peopleFunnel(List.of(), List.of(buy(1, 1, 500)));
        assertThat(orphan.get(3)).isEqualTo(new FunnelStep("BOUGHT", 1, null));
    }

    @Test
    @DisplayName("время до покупки — медиана минут от первого действия до первой покупки")
    void minutesToPurchase() {
        List<LogRow> log = List.of(
                row(1, 1, "VIEW", at("2026-10-21T07:00:00Z")),
                row(2, 1, "VIEW", at("2026-10-21T07:00:00Z")),
                row(1, 1, "PURCHASE_YES", at("2026-10-21T07:30:00Z")),
                row(1, 2, "PURCHASE_YES", at("2026-10-21T08:00:00Z")),
                row(2, 1, "PURCHASE_YES", at("2026-10-21T08:30:00Z")),
                row(3, 1, "VIEW", at("2026-10-21T07:00:00Z")));
        assertThat(MiniAppStats.medianMinutesToPurchase(log)).isEqualTo(new Median(60.0, 2));

        // Первое действие — вчера, покупка — сегодня: считается от вчерашнего.
        List<LogRow> twoDays = List.of(
                row(1, 1, "VIEW", at("2026-10-20T07:00:00Z")),
                row(1, 2, "VIEW", at("2026-10-21T07:00:00Z")),
                row(1, 2, "PURCHASE_YES", at("2026-10-21T07:10:00Z")));
        assertThat(MiniAppStats.medianMinutesToPurchase(twoDays)).isEqualTo(new Median(1450.0, 1));

        assertThat(MiniAppStats.medianMinutesToPurchase(List.of(row(1, 1, "LIKE"))))
                .isEqualTo(new Median(null, 0));
    }

    @Test
    @DisplayName("вернулись — действия в двух разных днях по Москве")
    void returningUsers() {
        List<LogRow> log = List.of(
                row(1, 1, "VIEW", at("2026-10-20T20:30:00Z")),
                row(1, 2, "VIEW", at("2026-10-20T21:30:00Z")),
                row(2, 1, "VIEW", at("2026-10-21T06:00:00Z")),
                row(2, 2, "VIEW", at("2026-10-21T09:00:00Z")));
        assertThat(MiniAppStats.returningUsers(log, MSK)).isEqualTo(new Ratio(1, 2, 50.0));
        assertThat(MiniAppStats.returningUsers(List.of(), MSK)).isEqualTo(new Ratio(0, 0, null));
    }

    @Test
    @DisplayName("карточек на человека — среднее число просмотров")
    void viewsPerUser() {
        List<LogRow> log = List.of(
                row(1, 1, "VIEW"), row(1, 2, "VIEW"), row(1, 3, "VIEW"), row(1, 4, "VIEW"),
                row(2, 1, "VIEW"), row(2, 2, "VIEW"), row(2, 3, "VIEW"), row(2, 3, "LIKE"),
                row(3, 1, "LIKE"));
        assertThat(MiniAppStats.viewsPerUser(log)).isEqualTo(2.3);
        assertThat(MiniAppStats.viewsPerUser(List.of())).isNull();
    }

    @Test
    @DisplayName("топ мероприятий — лайки плюс двойной вес покупок")
    void topByInterest() {
        List<LogRow> log = new ArrayList<>(List.of(
                row(1, 10, "LIKE"), row(2, 10, "LIKE"), row(3, 10, "LIKE"), row(3, 10, "LIKE"),
                row(1, 11, "LIKE"),
                row(1, 12, "LIKE"), row(2, 12, "LIKE"), row(3, 12, "LIKE"), row(4, 12, "LIKE"),
                row(1, 13, "VIEW")));
        List<BoughtRow> bought = List.of(buy(1, 10, 500), buy(1, 11, 500), buy(2, 11, 500));
        List<TopEvent> top = MiniAppStats.topByInterest(log, bought, 5);
        assertThat(top).containsExactly(
                new TopEvent(10, 3, 1, 5),
                new TopEvent(11, 1, 2, 5),
                new TopEvent(12, 4, 0, 4));

        for (int e = 20; e < 26; e++) {
            log.add(row(9, e, "LIKE"));
        }
        assertThat(MiniAppStats.topByInterest(log, bought, 5)).hasSize(5);
    }

    @Test
    @DisplayName("категории — уникальные «Пойду» и доля «не нравится» среди оценок")
    void byCategory() {
        List<LogRow> log = List.of(
                row(1, 1, "LIKE", "THEATRE", "moscow"),
                row(2, 1, "LIKE", "THEATRE", "moscow"),
                row(2, 1, "LIKE", "THEATRE", "moscow"),
                row(1, 2, "DISLIKE", "THEATRE", "moscow"),
                row(2, 2, "DISLIKE", "THEATRE", "moscow"),
                row(1, 3, "LIKE", "CONCERT", "kazan"),
                row(1, 4, "DISLIKE", "MUSEUM", "moscow"),
                row(1, 5, "LIKE", null, "moscow"),
                row(1, 6, "VIEW", "CINEMA", "moscow"));
        assertThat(MiniAppStats.byCategory(log)).containsExactly(
                new CategoryStat("THEATRE", 2, 2, 50.0),
                new CategoryStat("CONCERT", 1, 0, 0.0),
                new CategoryStat("OTHER", 1, 0, 0.0),
                new CategoryStat("MUSEUM", 0, 1, 100.0));
    }

    @Test
    @DisplayName("города — Москва и Казань всегда, «не указан» только при наличии")
    void byCity() {
        List<LogRow> log = List.of(
                row(1, 1, "VIEW", "THEATRE", "moscow"),
                row(1, 1, "LIKE", "THEATRE", "moscow"),
                row(1, 1, "LIKE", "THEATRE", "moscow"),
                row(2, 2, "VIEW", "THEATRE", "moscow"),
                row(3, 3, "LIKE", "THEATRE", "kazan"),
                row(4, 4, "VIEW", "THEATRE", null));
        List<BoughtRow> bought = List.of(buy(1, 1, 1000, "moscow"), buy(3, 3, 400, "kazan"));
        assertThat(MiniAppStats.byCity(log, bought)).containsExactly(
                new CityStat("moscow", 2, 1, 1, 1000),
                new CityStat("kazan", 1, 1, 1, 400),
                new CityStat(null, 1, 0, 0, 0));

        assertThat(MiniAppStats.byCity(List.of(row(1, 1, "VIEW")), List.of(buy(2, 2, 300, null))))
                .containsExactly(
                        new CityStat("moscow", 1, 0, 0, 0),
                        new CityStat("kazan", 0, 0, 0, 0),
                        new CityStat(null, 0, 0, 1, 300));

        assertThat(MiniAppStats.byCity(List.of(), List.of())).containsExactly(
                new CityStat("moscow", 0, 0, 0, 0),
                new CityStat("kazan", 0, 0, 0, 0));
    }

    @Test
    @DisplayName("медиана и процент — пустые значения и округление до 0,1")
    void medianAndPercent() {
        assertThat(MiniAppStats.median(List.of())).isNull();
        assertThat(MiniAppStats.median(List.of(5.0))).isEqualTo(5.0);
        assertThat(MiniAppStats.median(Arrays.asList(10.0, 1.0, 3.0, 2.0))).isEqualTo(2.5);
        assertThat(MiniAppStats.percent(1, 3)).isEqualTo(33.3);
        assertThat(MiniAppStats.percent(5, 0)).isNull();
    }

    @Test
    @DisplayName("пустая база — пустой ответ без исключений")
    void emptyDatabase() {
        MiniAppStats empty = MiniAppStats.compute(List.of(), List.of(), NOW);
        assertThat(empty).isEqualTo(new MiniAppStats(
                new Median(null, 0),
                new Money(0, 0, null),
                new Ratio(0, 0, null),
                new Activity(0, 0, 0),
                List.of(
                        new FunnelStep("OPENED", 0, null),
                        new FunnelStep("LIKED", 0, null),
                        new FunnelStep("CLICK_BUY", 0, null),
                        new FunnelStep("BOUGHT", 0, null)),
                new Median(null, 0),
                new Ratio(0, 0, null),
                null,
                List.of(),
                List.of(),
                List.of(new CityStat("moscow", 0, 0, 0, 0), new CityStat("kazan", 0, 0, 0, 0))));
    }

    @Test
    @DisplayName("JSON-контракт — все поля экрана и ни одного id пользователя")
    void jsonContract() {
        List<LogRow> log = List.of(
                row(990100, 1, "VIEW"), row(990100, 1, "LIKE"),
                row(990100, 1, "CLICK_BUY"), row(990100, 1, "PURCHASE_YES"));
        MiniAppStats stats = MiniAppStats.compute(log, List.of(buy(990100, 1, 700)), NOW);
        String json = new ObjectMapper().writeValueAsString(stats);
        assertThat(json).contains(
                "\"swipesToLike\"", "\"bought\"", "\"likeToBuy\"", "\"active\"", "\"funnel\"",
                "\"minutesToPurchase\"", "\"returned\"", "\"cardsPerUser\"", "\"topEvents\"",
                "\"categories\"", "\"cities\"");
        assertThat(json).doesNotContain("\"recent\"", "\"userId\"", "990100");
    }
}
