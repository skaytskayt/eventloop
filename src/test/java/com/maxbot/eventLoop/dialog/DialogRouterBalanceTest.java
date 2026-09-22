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

        AppProperties properties = new AppProperties(2000, 3000, "https://app.example/");
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
    @DisplayName("/start спрашивает кино и объясняет механику")
    void startAsksCinema() {
        send("/start");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastCardText()).contains("на кино").contains("вправо");
    }

    @Test
    @DisplayName("после кино спрашивается остальное, ссылка ещё не даётся")
    void cinemaThenOther() {
        send("/start");
        send("1200");
        assertThat(user.getCinemaRub()).isEqualTo(1200);
        assertThat(user.getOtherRub()).isZero();
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_OTHER);
        assertThat(lastText()).contains("1200").contains("остальное");
        verify(max, org.mockito.Mockito.never()).sendText(anyLong(), any(), org.mockito.ArgumentMatchers.notNull());
    }

    @Test
    @DisplayName("обе суммы названы — приходит ссылка с ними")
    void bothAmountsGiveTheLink() {
        send("/start");
        send("1200");
        send("2500");
        assertThat(user.getCinemaRub()).isEqualTo(1200);
        assertThat(user.getOtherRub()).isEqualTo(2500);
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);

        ArgumentCaptor<java.util.List<java.util.List<ru.max.botapi.model.Button>>> keyboard =
                ArgumentCaptor.forClass(java.util.List.class);
        verify(max, org.mockito.Mockito.atLeastOnce())
                .sendText(eq(CHAT_ID), any(), keyboard.capture());
        assertThat(keyboard.getValue()).isNotNull();
        assertThat(((ru.max.botapi.model.LinkButton) keyboard.getValue().getFirst().getFirst()).url())
                .isEqualTo("https://app.example/?cinema=1200&other=2500");
    }

    @Test
    @DisplayName("сумма больше кошелька не принимается, вопрос повторяется")
    void rejectsAmountAboveWalletLimit() {
        send("/start");
        send("4000");
        assertThat(user.getCinemaRub()).isZero();
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).contains("2000");
    }

    @Test
    @DisplayName("лимит второго кошелька свой: 2500 на кино нельзя, на остальное можно")
    void wallletsHaveOwnLimits() {
        send("/start");
        send("2000");
        send("2500");
        assertThat(user.getOtherRub()).isEqualTo(2500);
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
    }

    @Test
    @DisplayName("мусор вместо числа не двигает разговор")
    void garbageKeepsTheState() {
        send("/start");
        send("не помню");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).doesNotContain("Записал");
    }

    @Test
    @DisplayName("после готовности любой текст снова показывает ссылку")
    void readyStateRepeatsTheLink() {
        send("/start");
        send("1000");
        send("1000");
        send("привет");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(lastText()).contains("Записал");
    }

    @Test
    @DisplayName("/start начинает заново")
    void startResetsTheConversation() {
        send("/start");
        send("1000");
        send("1000");
        send("/start");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
    }

    @Test
    @DisplayName("сообщение от бота игнорируется")
    void ignoresBotMessages() {
        router.onUpdate(TestUpdates.botMessage(USER_ID, CHAT_ID, "1000"));
        verifyNoMoreInteractions(max);
    }
}
