package com.maxbot.eventLoop.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maxbot.eventLoop.config.AdminIds;
import com.maxbot.eventLoop.config.AppProperties;
import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.domain.DialogState;
import com.maxbot.eventLoop.gateway.MaxGateway;
import com.maxbot.eventLoop.gateway.WelcomeImage;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.OpenAppButton;

/**
 * Скрытая команда /stats: администратору — кнопка экрана статистики, остальным —
 * отказ с их MAX id. Команда не должна сбивать человека с шага анкеты.
 */
class DialogRouterStatsTest {

    private static final long USER_ID = 42L;
    private static final long CHAT_ID = 100L;

    private AppUserRepository users;
    private MaxGateway max;
    private AppUser user;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        max = mock(MaxGateway.class);
        user = new AppUser(USER_ID, CHAT_ID);
        when(users.findByMaxUserId(USER_ID)).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
    }

    private DialogRouter router(String adminIds, String botUsername) {
        AppProperties properties = new AppProperties(2000, 3000, botUsername);
        WelcomeImage welcome = mock(WelcomeImage.class);
        when(welcome.photos()).thenReturn(null);
        return new DialogRouter(users, new Screens(properties), max, welcome, properties,
                new AdminIds(adminIds));
    }

    private void send(DialogRouter router, String text) {
        router.onUpdate(TestUpdates.message(USER_ID, CHAT_ID, text));
    }

    private String lastText() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(max).sendText(eq(CHAT_ID), captor.capture(), any());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<List<Button>> lastKeyboard() {
        ArgumentCaptor<List<List<Button>>> captor = ArgumentCaptor.forClass(List.class);
        verify(max).sendText(eq(CHAT_ID), any(), captor.capture());
        return captor.getValue();
    }

    private void ready() {
        user.setDialogState(DialogState.READY);
        user.setCinemaRub(1200);
        user.setOtherRub(2500);
    }

    @Test
    @DisplayName("администратор получает кнопку статистики, анкета не меняется")
    void adminGetsButton() {
        ready();
        send(router("42", "test_bot"), "/stats");

        assertThat(lastText()).contains("Статистика EventLoop");
        List<List<Button>> keyboard = lastKeyboard();
        assertThat(keyboard).hasSize(1);
        assertThat(keyboard.getFirst()).hasSize(1);
        OpenAppButton button = (OpenAppButton) keyboard.getFirst().getFirst();
        assertThat(button.webApp()).isEqualTo("test_bot");
        assertThat(button.payload()).isEqualTo("stats");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getCinemaRub()).isEqualTo(1200);
        assertThat(user.getOtherRub()).isEqualTo(2500);
    }

    @Test
    @DisplayName("на шаге суммы /stats не считается ошибкой ввода")
    void adminWhileAwaitingCinema() {
        user.setDialogState(DialogState.AWAITING_CINEMA);
        user.setCinemaRub(0);
        user.setOtherRub(3700);
        send(router("42", "test_bot"), "/stats");

        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(user.getCinemaRub()).isEqualTo(0);
        assertThat(user.getOtherRub()).isEqualTo(3700);
        assertThat(lastText()).contains("Статистика EventLoop");
    }

    @Test
    @DisplayName("на шаге точки /stats точку не выбирает")
    void adminWhileAwaitingOrigin() {
        user.setDialogState(DialogState.AWAITING_ORIGIN);
        send(router("42", "test_bot"), "/stats");

        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        assertThat(user.getOriginName()).isNull();
        assertThat(lastText()).contains("Статистика EventLoop");
    }

    @Test
    @DisplayName("не администратору — «такой команды нет», без id и без кнопки")
    void notAdmin() {
        ready();
        send(router("777", "test_bot"), "/stats");

        assertThat(lastText()).isEqualTo("Такой команды нет.")
                .doesNotContain("42")
                .doesNotContain("ADMIN");
        assertThat(lastKeyboard()).isNull();
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
    }

    @Test
    @DisplayName("пустой ADMIN_MAX_IDS — тот же отказ")
    void emptyAdmins() {
        ready();
        send(router("", "test_bot"), "/stats");

        assertThat(lastText()).isEqualTo("Такой команды нет.");
        assertThat(lastKeyboard()).isNull();
    }

    @Test
    @DisplayName("без username бота администратору — текст без кнопки")
    void noBotUsername() {
        ready();
        send(router("42", null), "/stats");

        assertThat(lastText()).contains("Статистика EventLoop").contains("MAX\\_BOT\\_USERNAME");
        assertThat(lastKeyboard()).isNull();
    }
}
