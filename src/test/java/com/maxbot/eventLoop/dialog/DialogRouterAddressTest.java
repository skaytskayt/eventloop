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
import ru.max.botapi.model.RequestGeoLocationButton;

/**
 * /address — сменить точку, не вводя баланс заново: суммы остаются, бот снова
 * спрашивает геолокацию или адрес.
 */
class DialogRouterAddressTest {

    private static final long USER_ID = 42L;
    private static final long CHAT_ID = 100L;

    private AppUserRepository users;
    private MaxGateway max;
    private AppUser user;
    private DialogRouter router;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        max = mock(MaxGateway.class);
        user = new AppUser(USER_ID, CHAT_ID);
        when(users.findByMaxUserId(USER_ID)).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
        AppProperties properties = new AppProperties(2000, 3000, "test_bot");
        WelcomeImage welcome = mock(WelcomeImage.class);
        router = new DialogRouter(users, new Screens(properties), max, welcome, properties,
                new AdminIds(""), q -> Optional.empty());
    }

    private void send(String text) {
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

    @Test
    @DisplayName("после анкеты — снова вопрос про точку, суммы на месте")
    void fromReady() {
        user.setDialogState(DialogState.READY);
        user.setCinemaRub(1200);
        user.setOtherRub(2500);
        send("/address");

        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        assertThat(user.getCinemaRub()).isEqualTo(1200);
        assertThat(user.getOtherRub()).isEqualTo(2500);
        assertThat(lastText()).contains("Откуда поедешь");
        assertThat(lastKeyboard().getFirst().getFirst()).isInstanceOf(RequestGeoLocationButton.class);
    }

    @Test
    @DisplayName("до баланса — просим сначала назвать сумму, шаг не меняется")
    void tooEarly() {
        user.setDialogState(DialogState.AWAITING_CINEMA);
        user.setOtherRub(3700);
        send("/address");

        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).contains("Сначала скажи, сколько на карте");
    }
}
