package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.maxbot.eventLoop.config.AdminIds;
import com.maxbot.eventLoop.repo.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Статистика — метрики пилота по всем пользователям. Её видит только
 * администратор с подписанным initData; остальные получают 401/403, и хранилище
 * даже не опрашивается.
 */
class MiniAppControllerStatsTest {

    private InitDataVerifier verifier;
    private MiniAppStore store;
    private MiniAppStats sample;

    @BeforeEach
    void setUp() {
        verifier = mock(InitDataVerifier.class);
        store = mock(MiniAppStore.class);
        when(verifier.userId(null)).thenReturn(OptionalLong.empty());
        when(verifier.userId("forged")).thenReturn(OptionalLong.empty());
        when(verifier.userId("user42")).thenReturn(OptionalLong.of(42));
        when(verifier.userId("admin")).thenReturn(OptionalLong.of(777));
        sample = MiniAppStats.compute(List.of(), List.of(), Instant.now());
    }

    private MiniAppController controller(String adminIds) {
        return new MiniAppController(verifier, store, new AdminIds(adminIds), mock(AppUserRepository.class));
    }

    @Test
    @DisplayName("без заголовка — 401")
    void noHeader() {
        ResponseEntity<MiniAppStats> r = controller("777").stats(null);
        assertThat(r.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("битая подпись — 401")
    void forged() {
        ResponseEntity<MiniAppStats> r = controller("777").stats("forged");
        assertThat(r.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("валидная подпись не администратора — 403")
    void notAdmin() {
        ResponseEntity<MiniAppStats> r = controller("777").stats("user42");
        assertThat(r.getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("администратор получает агрегаты")
    void admin() {
        when(store.stats()).thenReturn(sample);
        ResponseEntity<MiniAppStats> r = controller("777").stats("admin");
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(sample);
    }

    @Test
    @DisplayName("пустой ADMIN_MAX_IDS — администраторов нет, 403")
    void emptyAdmins() {
        ResponseEntity<MiniAppStats> r = controller("").stats("admin");
        assertThat(r.getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(store);
    }
}
