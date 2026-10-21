package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maxbot.eventLoop.miniapp.MiniAppController.ActionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MiniAppActionTest {

    @Test
    @DisplayName("оценки и покупка переводят реакцию в своё состояние")
    void transitions() {
        assertThat(MiniAppAction.LIKE.reactionKind()).isEqualTo("LIKED");
        assertThat(MiniAppAction.SAVE.reactionKind()).isEqualTo("SAVED");
        assertThat(MiniAppAction.DISLIKE.reactionKind()).isEqualTo("DISLIKED");
        assertThat(MiniAppAction.PURCHASE_YES.reactionKind()).isEqualTo("BOUGHT");
        assertThat(MiniAppAction.PURCHASE_CANCEL.reactionKind()).isEqualTo("LIKED");
    }

    @Test
    @DisplayName("UNDO очищает реакцию, просмотр и переходы её не трогают")
    void neutralActions() {
        assertThat(MiniAppAction.UNDO.reactionKind()).isNull();
        assertThat(MiniAppAction.UNDO.clearsReaction()).isTrue();
        for (MiniAppAction a : new MiniAppAction[] {
                MiniAppAction.VIEW, MiniAppAction.CLICK_BUY, MiniAppAction.PURCHASE_NO}) {
            assertThat(a.reactionKind()).as(a.name()).isNull();
            assertThat(a.clearsReaction()).as(a.name()).isFalse();
        }
        assertThat(MiniAppAction.LIKE.clearsReaction()).isFalse();
    }

    @Test
    @DisplayName("тело действия проверяется при создании")
    void requestValidation() {
        assertThat(new ActionRequest(201, MiniAppAction.LIKE, 500, null, null).category()).isNull();
        assertThat(new ActionRequest(201, MiniAppAction.LIKE, 0, "THEATRE", null).priceRub()).isZero();
        assertThatThrownBy(() -> new ActionRequest(0, MiniAppAction.LIKE, 500, "THEATRE", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionRequest(201, null, 500, "THEATRE", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionRequest(201, MiniAppAction.LIKE, -1, "THEATRE", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionRequest(201, MiniAppAction.LIKE, 100001, "THEATRE", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActionRequest(201, MiniAppAction.LIKE, 500, "theatre", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("город необязателен, но только moscow или kazan")
    void cityValidation() {
        assertThat(new ActionRequest(201, MiniAppAction.LIKE, 500, "THEATRE", null).city()).isNull();
        assertThat(new ActionRequest(201, MiniAppAction.LIKE, 500, "THEATRE", "moscow").city())
                .isEqualTo("moscow");
        assertThat(new ActionRequest(201, MiniAppAction.LIKE, 500, "THEATRE", "kazan").city())
                .isEqualTo("kazan");
        for (String bad : new String[] {"spb", "MOSCOW", ""}) {
            assertThatThrownBy(() -> new ActionRequest(201, MiniAppAction.LIKE, 500, "THEATRE", bad))
                    .as(bad)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
