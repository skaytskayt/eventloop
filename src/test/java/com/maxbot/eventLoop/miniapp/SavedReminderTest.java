package com.maxbot.eventLoop.miniapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.miniapp.MiniAppController.ActionRequest;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SavedReminderTest {

    @Test
    @DisplayName("напоминание называет событие и время по Москве")
    void text() {
        String text = SavedReminder.text("Гамлет", Instant.parse("2026-10-27T16:00:00Z"));
        assertThat(text).contains("**Гамлет**").contains("27 октября в 19:00");
    }

    @Test
    @DisplayName("разметка в названии экранируется")
    void escapesMarkdown() {
        assertThat(SavedReminder.text("a*b_c", Instant.parse("2026-10-27T16:00:00Z")))
                .contains("a\\*b\\_c");
    }

    @Test
    @DisplayName("дата и название приходят в теле действия, старый клиент без них тоже принимается")
    void parsesStartsAt() {
        JsonMapper json = JsonMapper.builder().build();
        ActionRequest full = json.readValue("""
                {"eventId":5,"action":"SAVE","priceRub":500,"category":"THEATRE","city":"moscow",
                 "startsAt":"2026-10-27T19:00:00+03:00","title":"Гамлет"}
                """, ActionRequest.class);
        assertThat(full.startsAt()).isEqualTo(OffsetDateTime.parse("2026-10-27T19:00:00+03:00"));
        assertThat(full.title()).isEqualTo("Гамлет");

        ActionRequest old = json.readValue("""
                {"eventId":5,"action":"SAVE","priceRub":500}
                """, ActionRequest.class);
        assertThat(old.startsAt()).isNull();
        assertThat(old.title()).isNull();
    }
}
