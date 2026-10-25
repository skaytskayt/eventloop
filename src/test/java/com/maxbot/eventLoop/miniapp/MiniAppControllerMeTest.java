package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maxbot.eventLoop.config.AdminIds;
import com.maxbot.eventLoop.config.AppProperties;
import com.maxbot.eventLoop.dialog.Screens;
import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.domain.DialogState;
import com.maxbot.eventLoop.geo.AddressGeocoder;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Зарегистрирован — значит, прошёл анкету в боте до конца. Без этого
 * приложение просит сначала заглянуть в бот.
 */
class MiniAppControllerMeTest {

    private InitDataVerifier verifier;
    private AppUserRepository users;
    private MiniAppController controller;

    @BeforeEach
    void setUp() {
        verifier = mock(InitDataVerifier.class);
        users = mock(AppUserRepository.class);
        when(verifier.userId(null)).thenReturn(OptionalLong.empty());
        when(verifier.userId("user42")).thenReturn(OptionalLong.of(42));
        // Геокодер-заглушка: знает один казанский адрес и один — в Самаре, за 100 км от обоих городов.
        AddressGeocoder geocoder = q -> switch (q) {
            case "Казань, Баумана 10" -> Optional.of(new AddressGeocoder.Place("улица Баумана, 10, Казань", 55.78881, 49.11789));
            case "Самара" -> Optional.of(new AddressGeocoder.Place("Самара", 53.1959, 50.1002));
            case "Казань" -> Optional.of(new AddressGeocoder.Place("Казань", 55.7963, 49.1088, true));
            default -> Optional.empty();
        };
        controller = new MiniAppController(verifier, mock(MiniAppStore.class), new AdminIds(""), users,
                geocoder, new Screens(new AppProperties(2000, 3000, "test_bot")));
    }

    private AppUser userIn(DialogState state) {
        AppUser u = new AppUser(42, 1L);
        u.setDialogState(state);
        return u;
    }

    @Test
    @DisplayName("без подписи — 401")
    void noHeader() {
        assertThat(controller.me(null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("в боте ни разу не был — не зарегистрирован")
    void unknown() {
        when(users.findByMaxUserId(42)).thenReturn(Optional.empty());
        ResponseEntity<MiniAppController.Me> r = controller.me("user42");
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody().registered()).isFalse();
    }

    @Test
    @DisplayName("бросил анкету на середине — не зарегистрирован")
    void halfway() {
        when(users.findByMaxUserId(42)).thenReturn(Optional.of(userIn(DialogState.AWAITING_ORIGIN)));
        assertThat(controller.me("user42").getBody().registered()).isFalse();
    }

    @Test
    @DisplayName("прошёл анкету — зарегистрирован")
    void ready() {
        when(users.findByMaxUserId(42)).thenReturn(Optional.of(userIn(DialogState.READY)));
        assertThat(controller.me("user42").getBody().registered()).isTrue();
    }

    private ResponseEntity<MiniAppController.OriginResponse> origin(String address, Double lat, Double lon) {
        return controller.origin("user42", new MiniAppController.OriginRequest(address, lat, lon));
    }

    @Test
    @DisplayName("смена точки: адрес найден — сохраняется в профиль бота")
    void originByAddress() {
        AppUser u = userIn(DialogState.READY);
        when(users.findByMaxUserId(42)).thenReturn(Optional.of(u));

        ResponseEntity<MiniAppController.OriginResponse> r = origin("Казань, Баумана 10", null, null);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody().name()).isEqualTo("улица Баумана, 10, Казань");
        assertThat(u.getOriginLat()).isEqualTo(55.7888);
        assertThat(u.getOriginLon()).isEqualTo(49.1179);
        verify(users).save(u);
    }

    @Test
    @DisplayName("смена точки: не найден или дальше 100 км — 404, точка прежняя")
    void originNotFound() {
        AppUser u = userIn(DialogState.READY);
        when(users.findByMaxUserId(42)).thenReturn(Optional.of(u));

        assertThat(origin("Нигдевиль", null, null).getStatusCode().value()).isEqualTo(404);
        assertThat(origin("Самара", null, null).getStatusCode().value()).isEqualTo(404);
        assertThat(origin("12345", null, null).getStatusCode().value()).isEqualTo(404);
        assertThat(u.getOriginLat()).isNull();
    }

    @Test
    @DisplayName("смена точки: только город — 422, точка прежняя")
    void originCityOnly() {
        AppUser u = userIn(DialogState.READY);
        when(users.findByMaxUserId(42)).thenReturn(Optional.of(u));

        assertThat(origin("Казань", null, null).getStatusCode().value()).isEqualTo(422);
        assertThat(u.getOriginLat()).isNull();
    }

    @Test
    @DisplayName("смена точки геолокацией: рядом — как есть, далеко — центр ближайшего города")
    void originByCoordinates() {
        AppUser u = userIn(DialogState.READY);
        when(users.findByMaxUserId(42)).thenReturn(Optional.of(u));

        assertThat(origin(null, 55.751244, 37.618423).getBody().name()).isEqualTo(Screens.GEO_NAME);
        assertThat(u.getOriginLat()).isEqualTo(55.7512);

        assertThat(origin(null, 53.1959, 50.1002).getBody().name()).isEqualTo(Screens.KAZAN_CENTER.name());
    }

    @Test
    @DisplayName("смена точки без анкеты — 409")
    void originUnregistered() {
        when(users.findByMaxUserId(42)).thenReturn(Optional.of(userIn(DialogState.AWAITING_CINEMA)));
        assertThat(origin("Казань, Баумана 10", null, null).getStatusCode().value()).isEqualTo(409);
    }
}
