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
import com.maxbot.eventLoop.geo.AddressGeocoder;
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
    private static final String ASK_ORIGIN = "📍 Откуда поедешь? Поделись геолокацией кнопкой ниже или напиши "
            + "адрес — например, «Казань, Баумана 10» или просто «Москва, Арбат».";
    private static final String NOT_FOUND = "Не нашёл такой адрес в Москве или Казани. "
            + "Попробуй точнее — улица и дом — или отправь геолокацию.";
    private static final AddressGeocoder.Place BAUMANA =
            new AddressGeocoder.Place("улица Баумана, 7/10, Казань", 55.7934295, 49.1098114);

    private AppUserRepository users;
    private MaxGateway max;
    private AddressGeocoder geocoder;
    private DialogRouter router;
    private AppUser user;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        max = mock(MaxGateway.class);
        geocoder = mock(AddressGeocoder.class);
        when(geocoder.geocode(any())).thenReturn(Optional.empty());
        user = new AppUser(USER_ID, CHAT_ID);
        when(users.findByMaxUserId(USER_ID)).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppProperties properties = new AppProperties(2000, 3000, "test_bot");
        // Картинка приветствия грузится в MAX; в тесте её нет, и это штатный
        // случай — приветствие обязано уходить и без неё.
        WelcomeImage welcome = mock(WelcomeImage.class);
        when(welcome.photos()).thenReturn(null);
        router = new DialogRouter(users, new Screens(properties), max, welcome, properties,
                new AdminIds(""), geocoder);
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
        assertThat(lastCardText()).contains("Сколько всего денег на карте?").contains("вправо");
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
        sendLocation(55.79, 49.12);
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(lastText()).contains("мини-приложении").doesNotContain("Записал");

        ArgumentCaptor<java.util.List<java.util.List<ru.max.botapi.model.Button>>> keyboard =
                ArgumentCaptor.forClass(java.util.List.class);
        verify(max, org.mockito.Mockito.atLeastOnce())
                .sendText(eq(CHAT_ID), any(), keyboard.capture());
        assertThat(keyboard.getValue()).isNotNull();
        var button = (ru.max.botapi.model.OpenAppButton) keyboard.getValue().getFirst().getFirst();
        assertThat(button.webApp()).isEqualTo("test_bot");
        assertThat(button.payload()).isEqualTo("1200_2500_557900_491200");
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
        send("3000");
        send("3000");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_CINEMA);
        assertThat(lastText()).isEqualTo("При 3000 ₽ на карте на кино может быть не больше 2000 ₽. Сколько на кино?");
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
        sendLocation(55.79, 49.12);
        send("привет");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(lastText()).contains("мини-приложении");
    }

    /** Вопрос о точке: ровно одна кнопка геолокации, без точек-пресетов. */
    private void assertOriginQuestionKeyboard() {
        var keyboard = lastKeyboard();
        assertThat(keyboard).hasSize(1);
        assertThat(keyboard.getFirst()).hasSize(1);
        assertThat(keyboard.getFirst().getFirst()).isInstanceOf(ru.max.botapi.model.RequestGeoLocationButton.class);
        var geo = (ru.max.botapi.model.RequestGeoLocationButton) keyboard.getFirst().getFirst();
        assertThat(geo.text()).isEqualTo("📍 Отправить геолокацию");
    }

    private java.util.List<String> allTexts() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(max, org.mockito.Mockito.atLeastOnce()).sendText(eq(CHAT_ID), captor.capture(), any());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("после суммы на кино спрашивается точка: одна кнопка геолокации или адрес текстом")
    void cinemaThenOriginQuestion() {
        untilOrigin();
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        assertThat(lastText()).isEqualTo(ASK_ORIGIN);
        assertOriginQuestionKeyboard();
        verify(max, org.mockito.Mockito.never())
                .sendText(anyLong(), any(), org.mockito.ArgumentMatchers.argThat(k -> k != null
                        && k.stream().flatMap(java.util.List::stream)
                        .anyMatch(ru.max.botapi.model.MessageButton.class::isInstance)));
    }

    @Test
    @DisplayName("адрес найден — сохраняется, бот говорит, что нашёл, и даёт кнопку с координатами")
    void originByAddress() {
        when(geocoder.geocode("Казань, Баумана 10")).thenReturn(Optional.of(BAUMANA));
        untilOrigin();
        send("Казань, Баумана 10");
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getOriginName()).isEqualTo("улица Баумана, 7/10, Казань");
        assertThat(user.getOriginLat()).isEqualTo(55.7934);
        assertThat(user.getOriginLon()).isEqualTo(49.1098);
        assertThat(allTexts()).contains("Нашёл: улица Баумана, 7/10, Казань. Считаю расстояние отсюда.");
        assertThat(lastPayload()).isEqualTo("1200_2500_557934_491098");
    }

    private void assertNotFound() {
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        assertThat(user.getOriginName()).isNull();
        assertThat(user.getOriginLat()).isNull();
        assertThat(lastText()).isEqualTo(NOT_FOUND);
        assertOriginQuestionKeyboard();
    }

    @Test
    @DisplayName("адрес не найден — бот говорит об этом и остаётся на шаге точки")
    void addressNotFound() {
        untilOrigin();
        send("Хогвартс, Тайная комната");
        assertNotFound();
    }

    @Test
    @DisplayName("адрес дальше 100 км от обоих городов — как не найденный")
    void addressTooFar() {
        when(geocoder.geocode(any())).thenReturn(
                Optional.of(new AddressGeocoder.Place("Невский проспект, 1, Санкт-Петербург", 59.9343, 30.3351)));
        untilOrigin();
        send("Санкт-Петербург, Невский 1");
        assertNotFound();
    }

    @Test
    @DisplayName("только город — бот просит улицу и дом и остаётся на шаге точки")
    void cityOnlyAsksForPreciseAddress() {
        when(geocoder.geocode(any())).thenReturn(
                Optional.of(new AddressGeocoder.Place("Казань", 55.7963, 49.1088, true)));
        untilOrigin();
        send("Казань");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_ORIGIN);
        assertThat(user.getOriginLat()).isNull();
        assertThat(lastText()).contains("это весь город").contains("улицу и дом");
        assertOriginQuestionKeyboard();
    }

    @Test
    @DisplayName("сбой геокодера — ответ «не нашёл», реплика не теряется")
    void geocoderFailure() {
        when(geocoder.geocode(any())).thenThrow(new IllegalStateException("boom"));
        untilOrigin();
        send("Москва, Арбат");
        assertNotFound();
    }

    @Test
    @DisplayName("число на шаге точки в геокодер не уходит")
    void numberIsNotSentToGeocoder() {
        untilOrigin();
        send("2000");
        verify(geocoder, org.mockito.Mockito.never()).geocode(any());
        assertNotFound();
    }

    @Test
    @DisplayName("«Пропустить» больше не особенный — это просто текст для поиска")
    void skipIsJustText() {
        untilOrigin();
        send("Пропустить");
        verify(geocoder).geocode("Пропустить");
        assertNotFound();
    }

    @Test
    @DisplayName("/start на шаге точки начинает заново, геокодер не вызывается")
    void startWinsOverGeocoder() {
        untilOrigin();
        send("/start");
        assertThat(user.getDialogState()).isEqualTo(DialogState.AWAITING_TOTAL);
        verify(geocoder, org.mockito.Mockito.never()).geocode(any());
    }

    @Test
    @DisplayName("после готовности текст не геокодируется — точка не меняется, снова кнопка приложения")
    void readyDoesNotGeocode() {
        when(geocoder.geocode(any())).thenReturn(Optional.of(BAUMANA));
        untilOrigin();
        sendLocation(55.79, 49.12);
        send("привет");
        send("Москва, Арбат");
        verify(geocoder, org.mockito.Mockito.never()).geocode(any());
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getOriginName()).isEqualTo("По геолокации");
        assertThat(lastPayload()).isEqualTo("1200_2500_557900_491200");
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
    @DisplayName("непонятный ответ на вопрос о точке — «не нашёл» и снова кнопка геолокации")
    void garbageOriginRepeatsTheQuestion() {
        untilOrigin();
        send("где-то тут");
        assertNotFound();
    }

    @Test
    @DisplayName("после готовности точку можно сменить геолокацией")
    void readyStateChangesOrigin() {
        untilOrigin();
        sendLocation(54.7388, 55.9721);
        assertThat(user.getDialogState()).isEqualTo(DialogState.READY);
        assertThat(user.getOriginName()).isEqualTo("Казань · центр");
        assertThat(lastPayload()).isEqualTo("1200_2500_557961_491064");
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
