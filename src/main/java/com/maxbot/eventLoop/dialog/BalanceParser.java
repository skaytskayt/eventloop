package com.maxbot.eventLoop.dialog;

import java.util.Optional;

/**
 * Разбор суммы из свободного текста. Люди пишут «5000», «5 000 ₽», «5000р»,
 * «5 000,00» — и всё это должно пониматься, а мусор не должен ронять диалог.
 */
public final class BalanceParser {

    private BalanceParser() {
    }

    public sealed interface Result {

        record Ok(int rubles) implements Result {
        }

        record Error(String message) implements Result {
        }
    }

    public static Result parse(String input, int maxRubles) {
        if (input == null || input.isBlank()) {
            return new Result.Error("Не вижу числа. Напиши сумму цифрами, например 5000.");
        }

        String cleaned = input.trim()
                .replace(' ', ' ')       // неразрывный пробел из копипасты
                .replaceAll("[\\s_]", "")
                .replaceAll("[₽рРrRub]+$", "")
                .replaceAll("(,|\\.)\\d{1,2}$", ""); // копейки отбрасываем

        if (cleaned.startsWith("-")) {
            return new Result.Error("Баланс не может быть отрицательным. Сколько на карте сейчас?");
        }
        if (!cleaned.matches("\\d+")) {
            return new Result.Error("Не получилось разобрать сумму. Напиши только цифры, например 3500.");
        }

        long value;
        try {
            value = Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return new Result.Error("Слишком длинное число. Напиши сумму, которая реально на карте.");
        }
        if (value > maxRubles) {
            return new Result.Error("На Пушкинской карте не может быть больше " + maxRubles
                    + " ₽. Проверь сумму.");
        }
        return new Result.Ok((int) value);
    }

    public static Optional<Integer> parseOrEmpty(String input, int maxRubles) {
        return parse(input, maxRubles) instanceof Result.Ok ok
                ? Optional.of(ok.rubles())
                : Optional.empty();
    }
}
