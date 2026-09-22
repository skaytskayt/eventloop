package com.maxbot.eventLoop.dialog;

import ru.max.botapi.model.ChatType;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageBody;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageRecipient;
import ru.max.botapi.model.User;

/**
 * Апдейты MAX для тестов. Конструкторы записей SDK длинные и почти целиком
 * состоят из {@code null} — собраны здесь, чтобы тесты читались, а не тонули
 * в позиционных аргументах.
 */
final class TestUpdates {

    private TestUpdates() {
    }

    static MessageCreatedUpdate message(long userId, long chatId, String text) {
        return message(userId, chatId, text, false);
    }

    /** Эхо от самого бота: обрабатывать его нельзя, иначе диалог зациклится. */
    static MessageCreatedUpdate botMessage(long userId, long chatId, String text) {
        return message(userId, chatId, text, true);
    }

    private static MessageCreatedUpdate message(long userId, long chatId, String text, boolean bot) {
        User sender = new User(userId, "Тест", null, null, null, bot, null);
        MessageBody body = new MessageBody("mid-1", 1L, text, null, null);
        Message message = new Message(
                sender,
                new MessageRecipient(chatId, ChatType.DIALOG, null, null),
                0L, null, body, null, null, null);
        return new MessageCreatedUpdate(0L, message, null);
    }
}
