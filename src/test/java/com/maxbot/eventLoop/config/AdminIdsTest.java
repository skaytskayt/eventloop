package com.maxbot.eventLoop.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Список администраторов решает, кто видит статистику. Опечатка в .env не должна
 * открывать доступ, а пустой список — превращать всех в администраторов.
 */
class AdminIdsTest {

    @Test
    @DisplayName("id через запятую с пробелами и пустыми элементами")
    void parsesList() {
        AdminIds admins = new AdminIds("1, 2 ,,3");
        assertThat(admins.contains(1)).isTrue();
        assertThat(admins.contains(2)).isTrue();
        assertThat(admins.contains(3)).isTrue();
        assertThat(admins.contains(4)).isFalse();
    }

    @Test
    @DisplayName("пусто или null — администраторов нет")
    void emptyMeansNobody() {
        assertThat(new AdminIds("").isEmpty()).isTrue();
        assertThat(new AdminIds("").contains(1)).isFalse();
        assertThat(new AdminIds(null).isEmpty()).isTrue();
        assertThat(new AdminIds(null).contains(1)).isFalse();
    }

    @Test
    @DisplayName("мусор, ноль и отрицательные пропускаются и доступа не дают")
    void skipsGarbage() {
        AdminIds admins = new AdminIds("abc, -5, 0, 7");
        assertThat(admins.isEmpty()).isFalse();
        assertThat(admins.contains(7)).isTrue();
        assertThat(admins.contains(-5)).isFalse();
        assertThat(admins.contains(0)).isFalse();
    }
}
