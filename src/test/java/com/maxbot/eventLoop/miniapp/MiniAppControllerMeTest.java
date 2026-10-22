package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maxbot.eventLoop.config.AdminIds;
import com.maxbot.eventLoop.domain.AppUser;
import com.maxbot.eventLoop.domain.DialogState;
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
        controller = new MiniAppController(verifier, mock(MiniAppStore.class), new AdminIds(""), users);
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
}
