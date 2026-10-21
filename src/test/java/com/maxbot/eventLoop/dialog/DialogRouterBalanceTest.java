package com.maxbot.eventLoop.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.maxbot.eventLoop.config.AdminIds;
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
 * Разговор бота: две суммы и точка отсчёта — всё, что бот умеет. Ошибка здесь не падает
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
        router = new DialogRouter(users, new Screens(properties), max, welcome, properties,
                new AdminIds(""));
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
    private void sendLocation(double lat, double lon) {
        router.onUpdate(TestUpdates.location(USER_ID, CHAT_ID, lat, lon));
    }

    @SuppressWarnings("unchecked")
    private java.util.List<java.util.List<ru.max.botapi.model.Button>> lastKeyboard() {
        ArgumentCaptor<java.util.List<java.util.List<ru.max.botapi.model.Button>>> keyboard =
                ArgumentCaptor.forClass(java.util.List.class);
        verify(max, org.mockito.Mockito.atLeastOnce())
                .sendText(eq(CHAT_ID), any(), keyboard.capture());
        return keyboard.getValue();
    }

    private String lastPayload() {
        var button = (ru.max.botapi.model.OpenAppButton) lastKeyboard().getFirst().getFirst();
        return button.payload();
    }

    /** До вопроса о точке: обе суммы названы. */
    private void untilOrigin() {
        send("/start");
        send("3700");
        send("1200");
    }

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
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        send("Пропустить");
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
        assertThat(lastText()).contains("не больше 1000 ₽");
    }

    @Test
    @DisplayName("остальное не влезает в свой кошелёк — кино переспрашивается")
    void rejectsCinemaThatOverflowsOther() {
        send("/start");
        send("5000");
        send("1000");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).contains("только 2000 ₽");
        send("2000");
        assertThat(user.getCinemaRub()).isEqualTo(2000);
        assertThat(user.getOtherRub()).isEqualTo(3000);
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
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
        send("Пропустить");
        send("привет");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(lastText()).contains("мини-приложении");
    }

    @Test
    @DisplayName("после суммы на кино спрашивается точка отсчёта: геолокация, точки городов и «Пропустить»")
    void cinemaThenOriginQuestion() {
        untilOrigin();
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        assertThat(lastText()).contains("расстояние");
        var keyboard = lastKeyboard();
        assertThat(keyboard.getFirst().getFirst()).isInstanceOf(ru.max.botapi.model.RequestGeoLocationButton.class);
        var names = keyboard.stream().skip(1).flatMap(java.util.List::stream)
                .map(b -> ((ru.max.botapi.model.MessageButton) b).text()).toList();
        assertThat(names).containsAll(Screens.ORIGINS.stream().map(Screens.Origin::name).toList())
                .contains("Пропустить");
    }

    @Test
    @DisplayName("точка кнопкой — сохраняется, кнопка приложения несёт её координаты")
    void originByButton() {
        untilOrigin();
        send("Казань · центр");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getOriginName()).isEqualTo("Казань · центр");
        assertThat(user.getOriginLat()).isEqualTo(55.7961);
        assertThat(user.getOriginLon()).isEqualTo(49.1064);
        assertThat(lastPayload()).isEqualTo("1200_2500_557961_491064");
        assertThat(lastText()).contains("Казань · центр");
    }

    @Test
    @DisplayName("геолокация — сохраняется как «По геолокации», координаты в payload")
    void originByLocation() {
        untilOrigin();
        sendLocation(55.79, 49.12);
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getOriginName()).isEqualTo("По геолокации");
        assertThat(lastPayload()).isEqualTo("1200_2500_557900_491200");
    }

    @Test
    @DisplayName("геолокация далеко от обоих городов — точкой становится центр ближайшего")
    void farLocationFallsBackToNearestCenter() {
        untilOrigin();
        sendLocation(54.7388, 55.9721);
        assertThat(user.getOriginName()).isEqualTo("Казань · центр");
        assertThat(user.getOriginLat()).isEqualTo(55.7961);
        assertThat(user.getOriginLon()).isEqualTo(49.1064);
        assertThat(lastPayload()).isEqualTo("1200_2500_557961_491064");
    }

    @Test
    @DisplayName("«Пропустить» — точки нет, payload прежнего вида")
    void skipOrigin() {
        untilOrigin();
        send("Пропустить");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getOriginName()).isNull();
        assertThat(user.getOriginLat()).isNull();
        assertThat(lastPayload()).isEqualTo("1200_2500");
        assertThat(lastText()).contains("от центра Москвы");
    }

    @Test
    @DisplayName("непонятный ответ на вопрос о точке — вопрос повторяется")
    void garbageOriginRepeatsTheQuestion() {
        untilOrigin();
        send("где-то тут");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        assertThat(lastKeyboard().getFirst().getFirst())
                .isInstanceOf(ru.max.botapi.model.RequestGeoLocationButton.class);
    }

    @Test
    @DisplayName("после готовности точку можно сменить кнопкой или геолокацией")
    void readyStateChangesOrigin() {
        untilOrigin();
        send("Пропустить");
        send("Москва · м. ВДНХ");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getOriginName()).isEqualTo("Москва · м. ВДНХ");
        assertThat(lastPayload()).isEqualTo("1200_2500_558196_376408");
        sendLocation(55.79, 49.12);
        assertThat(user.getOriginName()).isEqualTo("По геолокации");
        assertThat(lastPayload()).isEqualTo("1200_2500_557900_491200");
        send("привет");
        assertThat(user.getOriginName()).isEqualTo("По геолокации");
        assertThat(lastPayload()).isEqualTo("1200_2500_557900_491200");
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
