package com.maxbot.eventLoop.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BalanceParserTest {

    private static final int MAX = 10_000;

    @ParameterizedTest(name = "«{0}» → {1} ₽")
    @CsvSource({
            "5000,5000",
            "'5 000',5000",
            "'5000 ₽',5000",
            "'5000₽',5000",
            "5000р,5000",
            "'  3500  ',3500",
            "'2 500,00',2500",
            "'1000.50',1000",
            "0,0",
            "10000,10000"
    })
    @DisplayName("разбирает форматы, которыми люди реально пишут сумму")
    void parsesRealWorldFormats(String input, int expected) {
        assertThat(BalanceParser.parse(input, MAX))
                .isInstanceOfSatisfying(BalanceParser.Result.Ok.class,
                        ok -> assertThat(ok.rubles()).isEqualTo(expected));
    }

    @ParameterizedTest(name = "«{0}» отклоняется с объяснением")
    @ValueSource(strings = {
            "-100",
            "пять тысяч",
            "abc",
            "",
            "   ",
            "10001",
            "99999999999999999999",
            "5000 рублей на карте",
            "?",
            "5к"
    })
    @DisplayName("мусорный ввод не роняет диалог и получает понятный ответ")
    void rejectsGarbageWithMessage(String input) {
        BalanceParser.Result result = BalanceParser.parse(input, MAX);
        assertThat(result).isInstanceOf(BalanceParser.Result.Error.class);
        assertThat(((BalanceParser.Result.Error) result).message()).isNotBlank();
    }

    @Test
    @DisplayName("null не бросает исключение")
    void handlesNull() {
        assertThat(BalanceParser.parse(null, MAX)).isInstanceOf(BalanceParser.Result.Error.class);
    }

    @Test
    @DisplayName("отрицательная сумма получает отдельное объяснение, а не общее")
    void negativeGetsItsOwnMessage() {
        BalanceParser.Result result = BalanceParser.parse("-500", MAX);
        assertThat(((BalanceParser.Result.Error) result).message()).contains("отрицательным");
    }

    @Test
    @DisplayName("сумма выше номинала карты отклоняется")
    void rejectsAboveNominal() {
        BalanceParser.Result result = BalanceParser.parse("50000", MAX);
        assertThat(((BalanceParser.Result.Error) result).message()).contains("10000");
    }
}
