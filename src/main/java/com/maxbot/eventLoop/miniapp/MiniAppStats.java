package com.maxbot.eventLoop.miniapp;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Метрики пилота для экрана статистики — только агрегаты, без id пользователей. */
public record MiniAppStats(
        Median swipesToLike,
        Money bought,
        Ratio likeToBuy,
        Activity active,
        List<FunnelStep> funnel,
        Median minutesToPurchase,
        Ratio returned,
        Double cardsPerUser,
        List<TopEvent> topEvents,
        List<CategoryStat> categories,
        List<CityStat> cities,
        BotUsers botUsers) {

    public static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    private static final List<String> CITIES = List.of("moscow", "kazan");

    /** Строка miniapp_action; список — в порядке id. */
    public record LogRow(long userId, int eventId, String action, int priceRub,
                         String category, String city, Instant at) {
    }

    /** Строка miniapp_reaction с kind = 'BOUGHT' — покупка, которую не отменили. */
    public record BoughtRow(long userId, int eventId, int priceRub, String category, String city) {
    }

    public record Median(Double value, long users) {
    }

    public record Money(long rub, long count, Long avgCheckRub) {
    }

    public record Ratio(long part, long whole, Double percent) {
    }

    public record Activity(long today, long week, long newToday) {
    }

    /**
     * Люди из app_user: написали боту и прошли ли анкету. Журнал приложения
     * их не видит, пока они не открыли карточку из MAX.
     */
    public record BotUsers(long total, long registered) {
    }

    /** step: OPENED | LIKED | CLICK_BUY | BOUGHT. */
    public record FunnelStep(String step, long users, Double percentOfFirst) {
    }

    public record TopEvent(int eventId, long likes, long purchases, long score) {
    }

    public record CategoryStat(String category, long likes, long dislikes, Double dislikePercent) {
    }

    /** city: moscow | kazan | null — действия без города. */
    public record CityStat(String city, long users, long likes, long purchases, long rub) {
    }

    private record Pair(long userId, int eventId) {
    }

    public static MiniAppStats compute(List<LogRow> log, List<BoughtRow> bought, BotUsers botUsers, Instant now) {
        return new MiniAppStats(
                medianViewsBeforeFirstLike(log),
                boughtTotals(bought),
                likedToBought(log, bought),
                activeUsers(log, now, MOSCOW),
                peopleFunnel(log, bought),
                medianMinutesToPurchase(log),
                returningUsers(log, MOSCOW),
                viewsPerUser(log),
                topByInterest(log, bought, 5),
                byCategory(log),
                byCity(log, bought),
                botUsers);
    }

    /** Сколько карточек человек пролистывает, прежде чем найти «своё» мероприятие. */
    static Median medianViewsBeforeFirstLike(List<LogRow> log) {
        Map<Long, Integer> views = new HashMap<>();
        Map<Long, Integer> firstLike = new LinkedHashMap<>();
        for (LogRow r : log) {
            if (firstLike.containsKey(r.userId())) {
                continue;
            }
            if ("LIKE".equals(r.action())) {
                firstLike.put(r.userId(), views.getOrDefault(r.userId(), 0));
            } else if ("VIEW".equals(r.action())) {
                views.merge(r.userId(), 1, Integer::sum);
            }
        }
        List<Double> counts = firstLike.values().stream().map(Integer::doubleValue).toList();
        return new Median(median(counts), counts.size());
    }

    /** Сколько денег с карты бот довёл до реальной покупки билетов. */
    static Money boughtTotals(List<BoughtRow> bought) {
        long rub = bought.stream().mapToLong(BoughtRow::priceRub).sum();
        long count = bought.size();
        return new Money(rub, count, count == 0 ? null : Math.round((double) rub / count));
    }

    /** Какая доля отметок «Пойду» превращается в купленный билет. */
    static Ratio likedToBought(List<LogRow> log, List<BoughtRow> bought) {
        Set<Pair> liked = pairs(log, "LIKE");
        Set<Pair> boughtPairs = new HashSet<>();
        for (BoughtRow b : bought) {
            boughtPairs.add(new Pair(b.userId(), b.eventId()));
        }
        long part = liked.stream().filter(boughtPairs::contains).count();
        return new Ratio(part, liked.size(), percent(part, liked.size()));
    }

    /** Сколько людей пользуются ботом сегодня и за неделю и сколько из них пришли впервые. */
    static Activity activeUsers(List<LogRow> log, Instant now, ZoneId zone) {
        LocalDate today = now.atZone(zone).toLocalDate();
        LocalDate weekStart = today.minusDays(6);
        Map<Long, LocalDate> first = new HashMap<>();
        Set<Long> todayUsers = new HashSet<>();
        Set<Long> weekUsers = new HashSet<>();
        for (LogRow r : log) {
            LocalDate day = r.at().atZone(zone).toLocalDate();
            first.merge(r.userId(), day, (a, b) -> a.isBefore(b) ? a : b);
            if (day.equals(today)) {
                todayUsers.add(r.userId());
            }
            if (!day.isBefore(weekStart) && !day.isAfter(today)) {
                weekUsers.add(r.userId());
            }
        }
        long newToday = first.values().stream().filter(today::equals).count();
        return new Activity(todayUsers.size(), weekUsers.size(), newToday);
    }

    /** Сколько людей доходит от открытия приложения до купленного билета. */
    static List<FunnelStep> peopleFunnel(List<LogRow> log, List<BoughtRow> bought) {
        long opened = log.stream().map(LogRow::userId).distinct().count();
        long liked = usersWith(log, "LIKE");
        long clicked = usersWith(log, "CLICK_BUY");
        long boughtUsers = bought.stream().map(BoughtRow::userId).distinct().count();
        return List.of(
                new FunnelStep("OPENED", opened, percent(opened, opened)),
                new FunnelStep("LIKED", liked, percent(liked, opened)),
                new FunnelStep("CLICK_BUY", clicked, percent(clicked, opened)),
                new FunnelStep("BOUGHT", boughtUsers, percent(boughtUsers, opened)));
    }

    /** Как быстро человек от первого касания доходит до покупки. */
    static Median medianMinutesToPurchase(List<LogRow> log) {
        Map<Long, Instant> first = new HashMap<>();
        Map<Long, Instant> firstPurchase = new LinkedHashMap<>();
        for (LogRow r : log) {
            first.merge(r.userId(), r.at(), MiniAppStats::earlier);
            if ("PURCHASE_YES".equals(r.action())) {
                firstPurchase.merge(r.userId(), r.at(), MiniAppStats::earlier);
            }
        }
        List<Double> minutes = new ArrayList<>();
        firstPurchase.forEach((user, at) ->
                minutes.add(Duration.between(first.get(user), at).toSeconds() / 60.0));
        return new Median(median(minutes), minutes.size());
    }

    /** Возвращаются ли люди к боту в другой день. */
    static Ratio returningUsers(List<LogRow> log, ZoneId zone) {
        Map<Long, Set<LocalDate>> days = new HashMap<>();
        for (LogRow r : log) {
            days.computeIfAbsent(r.userId(), u -> new HashSet<>()).add(r.at().atZone(zone).toLocalDate());
        }
        long returned = days.values().stream().filter(d -> d.size() >= 2).count();
        return new Ratio(returned, days.size(), percent(returned, days.size()));
    }

    /** Сколько карточек в среднем смотрит один человек. */
    static Double viewsPerUser(List<LogRow> log) {
        long users = log.stream().map(LogRow::userId).distinct().count();
        if (users == 0) {
            return null;
        }
        long views = log.stream().filter(r -> "VIEW".equals(r.action())).count();
        return round1((double) views / users);
    }

    /** Какие мероприятия цепляют сильнее всего: «Пойду» плюс двойной вес покупок. */
    static List<TopEvent> topByInterest(List<LogRow> log, List<BoughtRow> bought, int limit) {
        Map<Integer, Long> likes = new HashMap<>();
        for (Pair p : pairs(log, "LIKE")) {
            likes.merge(p.eventId(), 1L, Long::sum);
        }
        Map<Integer, Long> purchases = new HashMap<>();
        for (BoughtRow b : bought) {
            purchases.merge(b.eventId(), 1L, Long::sum);
        }
        Set<Integer> events = new HashSet<>(likes.keySet());
        events.addAll(purchases.keySet());
        return events.stream()
                .map(e -> {
                    long l = likes.getOrDefault(e, 0L);
                    long p = purchases.getOrDefault(e, 0L);
                    return new TopEvent(e, l, p, l + 2 * p);
                })
                .filter(t -> t.score() > 0)
                .sorted(Comparator.comparingLong(TopEvent::score).reversed()
                        .thenComparing(Comparator.comparingLong(TopEvent::likes).reversed())
                        .thenComparingInt(TopEvent::eventId))
                .limit(limit)
                .toList();
    }

    /** Какие категории нравятся и какие чаще отвергают. */
    static List<CategoryStat> byCategory(List<LogRow> log) {
        Map<String, Set<Pair>> likes = new TreeMap<>();
        Map<String, Set<Pair>> dislikes = new TreeMap<>();
        for (LogRow r : log) {
            String category = r.category() == null ? "OTHER" : r.category();
            Pair pair = new Pair(r.userId(), r.eventId());
            if ("LIKE".equals(r.action())) {
                likes.computeIfAbsent(category, c -> new HashSet<>()).add(pair);
            } else if ("DISLIKE".equals(r.action())) {
                dislikes.computeIfAbsent(category, c -> new HashSet<>()).add(pair);
            }
        }
        Set<String> categories = new HashSet<>(likes.keySet());
        categories.addAll(dislikes.keySet());
        return categories.stream()
                .map(c -> {
                    long l = likes.getOrDefault(c, Set.of()).size();
                    long d = dislikes.getOrDefault(c, Set.of()).size();
                    return new CategoryStat(c, l, d, percent(d, l + d));
                })
                .sorted(Comparator.comparingLong(CategoryStat::likes).reversed()
                        .thenComparing(CategoryStat::category))
                .toList();
    }

    /** Как пилот идёт в Москве и в Казани. */
    static List<CityStat> byCity(List<LogRow> log, List<BoughtRow> bought) {
        boolean unknown = log.stream().anyMatch(r -> r.city() == null)
                || bought.stream().anyMatch(b -> b.city() == null);
        List<String> cities = new ArrayList<>(CITIES);
        if (unknown) {
            cities.add(null);
        }
        List<CityStat> result = new ArrayList<>();
        for (String city : cities) {
            List<LogRow> rows = log.stream().filter(r -> Objects.equals(r.city(), city)).toList();
            List<BoughtRow> buys = bought.stream().filter(b -> Objects.equals(b.city(), city)).toList();
            result.add(new CityStat(city,
                    rows.stream().map(LogRow::userId).distinct().count(),
                    pairs(rows, "LIKE").size(),
                    buys.size(),
                    buys.stream().mapToLong(BoughtRow::priceRub).sum()));
        }
        return result;
    }

    /** Медиана с округлением до 0,1; null — значений нет. */
    static Double median(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        double m = sorted.size() % 2 == 1 ? sorted.get(mid) : (sorted.get(mid - 1) + sorted.get(mid)) / 2;
        return round1(m);
    }

    /** Процент с округлением до 0,1; null — знаменатель ноль. */
    static Double percent(long part, long whole) {
        return whole == 0 ? null : round1(part * 100.0 / whole);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static Instant earlier(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private static Set<Pair> pairs(List<LogRow> log, String action) {
        Set<Pair> result = new HashSet<>();
        for (LogRow r : log) {
            if (action.equals(r.action())) {
                result.add(new Pair(r.userId(), r.eventId()));
            }
        }
        return result;
    }

    private static long usersWith(List<LogRow> log, String action) {
        return log.stream().filter(r -> action.equals(r.action())).map(LogRow::userId).distinct().count();
    }
}
