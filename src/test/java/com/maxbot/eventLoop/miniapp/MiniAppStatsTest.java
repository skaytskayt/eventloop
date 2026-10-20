package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.miniapp.MiniAppStats.FunnelStep;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MiniAppStatsTest {

    @Test
    @DisplayName("проценты воронки считаются от предыдущего шага")
    void funnel() {
        List<FunnelStep> steps = MiniAppStats.funnel(40, 12, 5, 2);
        assertThat(steps).extracting(FunnelStep::step)
                .containsExactly("VIEW", "LIKE", "CLICK_BUY", "PURCHASE_YES");
        assertThat(steps).extracting(FunnelStep::count).containsExactly(40L, 12L, 5L, 2L);
        assertThat(steps).extracting(FunnelStep::percentOfPrevious)
                .containsExactly(100.0, 30.0, 41.7, 40.0);
        assertThat(MiniAppStats.overallPercent(40, 2)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("пустая база — нули без исключений")
    void empty() {
        assertThat(MiniAppStats.funnel(0, 0, 0, 0)).extracting(FunnelStep::percentOfPrevious)
                .containsExactly(0.0, 0.0, 0.0, 0.0);
        assertThat(MiniAppStats.overallPercent(0, 0)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("лайк без просмотра не даёт бесконечности")
    void divisionByZero() {
        assertThat(MiniAppStats.funnel(0, 3, 0, 0).get(1).percentOfPrevious()).isEqualTo(0.0);
    }
}
