package com.maxbot.eventLoop.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.maxbot.eventLoop.config.AppProperties;
import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.domain.DialogState;
import com.maxbot.eventLoop.gateway.MaxGateway;
import com.maxbot.eventLoop.gateway.WelcomeImage;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Двухшаговый разговор про деньги — всё, что бот умеет. Ошибка здесь не падает
 * исключением: человек просто застревает на вопросе или уходит в приложение
 * с чужой суммой.
 *
 * <p>SDK сюда не приезжает: роутер разбирает текст, а отправку берёт на себя
 * {@link MaxGateway}, который здесь подменён.
 */
class DialogRouterBalanceTest {

    private static final long USER_ID = 42L;
    private static final long CHAT_ID = 100L;

    private AppUserRepository users;
    private MaxGateway max;
    private DialogRouter router;
    private AppUser user;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        max = mock(MaxGateway.class);
        user = new AppUser(USER_ID, CHAT_ID);
        when(users.findByMaxUserId(USER_ID)).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppProperties properties = new AppProperties(2000, 3000, "test_bot");
        // Картинка приветствия грузится в MAX; в тесте её нет, и это штатный
        // случай — приветствие обязано уходить и без неё.
        WelcomeImage welcome = mock(WelcomeImage.class);
        when(welcome.photos()).thenReturn(null);
        router = new DialogRouter(users, new Screens(properties), max, welcome, properties);
    }

    /** Роутер вызывается из обработчика апдейтов; здесь дёргаем ту же ветку. */
    private void send(String text) {
        router.onUpdate(TestUpdates.message(USER_ID, CHAT_ID, text));
    }

    private String lastText() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(max, org.mockito.Mockito.atLeastOnce()).sendText(eq(CHAT_ID), captor.capture(), any());
        return captor.getValue();
    }

    /** Приветствие уходит картинкой, остальные реплики — текстом. */
    private String lastCardText() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(max, org.mockito.Mockito.atLeastOnce())
                .sendCard(eq(CHAT_ID), captor.capture(), any(), any());
        return captor.getValue();
    }

    @Test
    @DisplayName("/start спрашивает общий баланс и объясняет механику")
    void startAsksTotal() {
        send("/start");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_TOTAL);
        assertThat(lastCardText()).contains("Сколько всего на карте?").contains("вправо");
    }

    @Test
    @DisplayName("после общей суммы спрашивается кино, кнопка ещё не даётся")
    void totalThenCinema() {
        send("/start");
        send("4000");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).contains("4000").contains("на кино");
        verify(max, org.mockito.Mockito.never()).sendText(anyLong(), any(), org.mockito.ArgumentMatchers.notNull());
    }

    @Test
    @DisplayName("обе суммы названы — остальное считается разницей, приходит кнопка мини-приложения")
    void bothAmountsGiveTheButton() {
        send("/start");
        send("3700");
        send("1200");
        assertThat(user.getCinemaRub()).isEqualTo(1200);
        assertThat(user.getOtherRub()).isEqualTo(2500);
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(lastText()).contains("мини-приложении").doesNotContain("Записал");

        ArgumentCaptor<java.util.List<java.util.List<ru.max.botapi.model.Button>>> keyboard =
                ArgumentCaptor.forClass(java.util.List.class);
        verify(max, org.mockito.Mockito.atLeastOnce())
                .sendText(eq(CHAT_ID), any(), keyboard.capture());
        assertThat(keyboard.getValue()).isNotNull();
        var button = (ru.max.botapi.model.OpenAppButton) keyboard.getValue().getFirst().getFirst();
        assertThat(button.webApp()).isEqualTo("test_bot");
        assertThat(button.payload()).isEqualTo("1200_2500");
    }

    @Test
    @DisplayName("общая сумма больше номинала карты не принимается")
    void rejectsTotalAboveNominal() {
        send("/start");
        send("6000");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_TOTAL);
        assertThat(lastText()).contains("5000");
    }

    @Test
    @DisplayName("на кино больше номинала кошелька нельзя")
    void rejectsCinemaAboveWalletLimit() {
        send("/start");
        send("5000");
        send("2500");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).contains("2000");
    }

    @Test
    @DisplayName("на кино больше, чем всего на карте, нельзя")
    void rejectsCinemaAboveTotal() {
        send("/start");
        send("1000");
        send("1500");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).contains("от 0 до 1000");
    }

    @Test
    @DisplayName("остальное не влезает в свой кошелёк — кино переспрашивается")
    void rejectsCinemaThatOverflowsOther() {
        send("/start");
        send("5000");
        send("1000");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).contains("от 2000 до 2000");
        send("2000");
        assertThat(user.getCinemaRub()).isEqualTo(2000);
        assertThat(user.getOtherRub()).isEqualTo(3000);
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
    }

    @Test
    @DisplayName("мусор вместо числа не двигает разговор")
    void garbageKeepsTheState() {
        send("/start");
        send("не помню");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_TOTAL);
        assertThat(lastText()).doesNotContain("мини-приложении");
    }

    @Test
    @DisplayName("после готовности любой текст снова показывает кнопку")
    void readyStateRepeatsTheButton() {
        send("/start");
        send("2000");
        send("1000");
        send("привет");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(lastText()).contains("мини-приложении");
    }

    @Test
    @DisplayName("/start начинает заново")
    void startResetsTheConversation() {
        send("/start");
        send("2000");
        send("1000");
        send("/start");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_TOTAL);
    }

    @Test
    @DisplayName("сообщение от бота игнорируется")
    void ignoresBotMessages() {
        router.onUpdate(TestUpdates.botMessage(USER_ID, CHAT_ID, "1000"));
        verifyNoMoreInteractions(max);
    }
}
