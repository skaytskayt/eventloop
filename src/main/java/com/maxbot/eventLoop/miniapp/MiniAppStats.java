package com.maxbot.eventLoop.miniapp;

import java.util.List;

/** Агрегаты для страницы /stats. Ни одного id пользователя — только счётчики. */
public record MiniAppStats(
        long users,
        List<FunnelStep> funnel,
        double overallPercent,
        long boughtRub,
        long boughtCount,
        List<CategoryCount> likesByCategory,
        List<RecentAction> recent) {

    public record FunnelStep(String step, long count, double percentOfPrevious) {
    }

    public record CategoryCount(String category, long count) {
    }

    public record RecentAction(int eventId, String action, String category, int priceRub, long at) {
    }

    /** VIEW → LIKE → CLICK_BUY → PURCHASE_YES, процент — от предыдущего шага. */
    public static List<FunnelStep> funnel(long view, long like, long clickBuy, long purchaseYes) {
        return List.of(
                new FunnelStep("VIEW", view, view > 0 ? 100.0 : 0.0),
                new FunnelStep("LIKE", like, percent(like, view)),
                new FunnelStep("CLICK_BUY", clickBuy, percent(clickBuy, like)),
                new FunnelStep("PURCHASE_YES", purchaseYes, percent(purchaseYes, clickBuy)));
    }

    /** Сквозная конверсия от просмотра карточки до подтверждённой покупки. */
    public static double overallPercent(long view, long purchaseYes) {
        return percent(purchaseYes, view);
    }

    private static double percent(long part, long base) {
        return base > 0 ? Math.round(part * 1000.0 / base) / 10.0 : 0.0;
    }
}
