package com.maxbot.eventLoop.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.MessageButton;
import ru.max.botapi.model.OpenAppButton;
import ru.max.botapi.model.RequestGeoLocationButton;

/**
 * Кнопка мини-приложения — единственное, ради чего теперь существует бот.
 * Если она собрана неверно, человек попадает в приложение с чужими суммами
 * или не попадает вовсе, а в чате это выглядит как «кнопка не работает».
 */
class ScreensMiniAppTest {

    private static Screens screens(String bot) {
        return new Screens(new AppProperties(2000, 3000, bot));
    }

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
            "1200, 2500, 1200_2500",
            "0,    0,    0_0",
            "2000, 3000, 2000_3000"
    })
    @DisplayName("обе суммы уезжают в payload символами, которые MAX пропускает")
    void payloadCarriesBothWallets(int cinema, int other, String expected) {
        String payload = screens("bot").miniAppPayload(cinema, other);
        assertThat(payload).isEqualTo(expected).matches("[A-Za-z0-9_-]{1,512}");
    }

    @ParameterizedTest(name = "{0} + {1} @ {2},{3} → {4}")
    @CsvSource({
            "1200, 2500, 55.7961, 49.1064, 1200_2500_557961_491064",
            "2000, 3000, -33.92487, 18.42406, 2000_3000_-339249_184241",
            "2000, 3000, -89.99999, -179.99999, 2000_3000_-900000_-1800000"
    })
    @DisplayName("точка отсчёта уезжает в payload целыми числами и не раздувает его за 64 символа")
    void payloadCarriesOrigin(int cinema, int other, double lat, double lon, String expected) {
        String payload = screens("bot").miniAppPayload(cinema, other, lat, lon);
        assertThat(payload).isEqualTo(expected).matches("[A-Za-z0-9_-]+").hasSizeLessThanOrEqualTo(64);
    }

    @Test
    @DisplayName("без точки payload прежнего вида — старые ссылки и «Пропустить» работают")
    void payloadWithoutOriginKeepsOldFormat() {
        assertThat(screens("bot").miniAppPayload(1200, 2500, null, null)).isEqualTo("1200_2500");
    }

    @Test
    @DisplayName("клавиатура точки: геолокация первым рядом, затем точки городов и «Пропустить»")
    void originKeyboard() {
        List<List<Button>> keyboard = screens("bot").originKeyboard();
        assertThat(keyboard.getFirst()).hasSize(1);
        assertThat(keyboard.getFirst().getFirst()).isInstanceOf(RequestGeoLocationButton.class);
        List<String> names = keyboard.stream().skip(1).flatMap(List::stream)
                .map(b -> ((MessageButton) b).text()).toList();
        assertThat(names).contains("Москва · центр", "Казань · центр", "Пропустить");
        assertThat(names.getLast()).isEqualTo("Пропустить");
    }

    @Test
    @DisplayName("без username бота кнопки нет")
    void noBotNoButton() {
        assertThat(screens(null).miniAppKeyboard(1000, 1000)).isNull();
        assertThat(screens("").miniAppKeyboard(1000, 1000)).isNull();
    }

    @Test
    @DisplayName("кнопка открывает мини-приложение бота с обеими суммами")
    void keyboardOpensTheApp() {
        List<List<Button>> keyboard = screens("test_bot").miniAppKeyboard(500, 1500);
        assertThat(keyboard).hasSize(1);
        assertThat(keyboard.getFirst()).hasSize(1);
        assertThat(keyboard.getFirst().getFirst()).isInstanceOf(OpenAppButton.class);
        OpenAppButton button = (OpenAppButton) keyboard.getFirst().getFirst();
        assertThat(button.text()).isEqualTo("🎭 Открыть подборку");
        assertThat(button.webApp()).isEqualTo("test_bot");
        assertThat(button.payload()).isEqualTo("500_1500");
    }

    @ParameterizedTest(name = "всего {0} → кино от {1} до {2}")
    @CsvSource({
            "5000, 2000, 2000",
            "4000, 1000, 2000",
            "3000, 0,    2000",
            "1500, 0,    1500",
            "0,    0,    0"
    })
    @DisplayName("диапазон суммы на кино: оба кошелька должны влезть в свои номиналы")
    void cinemaRange(int total, int min, int max) {
        Screens s = screens(null);
        assertThat(s.minCinema(total)).isEqualTo(min);
        assertThat(s.maxCinema(total)).isEqualTo(max);
    }

    /**
     * Приветствие — единственное место, где человек узнаёт, что его ждёт.
     * Без механики оно превращается в «напиши число» без объяснения зачем.
     */
    @Test
    @DisplayName("приветствие объясняет механику и задаёт первый вопрос")
    void welcomeExplainsHowItWorks() {
        String welcome = screens("test_bot").welcome();
        assertThat(welcome)
                .contains("подборку")
                .contains("вправо")
                .contains("останется на карте")
                .contains("Купить билет")
                .contains("фильтры")
                .contains("на кино")
                .contains("Сколько всего на карте?");
    }

    @Test
    @DisplayName("второй вопрос повторяет общую сумму и называет потолок на кино")
    void secondQuestionRepeatsTheTotal() {
        assertThat(screens(null).askCinema(5000))
                .contains("Всего на карте: **5000 ₽**")
                .contains("Напиши сумму до 2000 ₽, остальное посчитаю сам.");
        assertThat(screens(null).askCinema(1000)).contains("до 1000 ₽");
    }

    @ParameterizedTest(name = "всего {0} → «{1}»")
    @CsvSource(delimiter = '|', value = {
            "5000 | только 2000 ₽",
            "4000 | от 1000 до 2000 ₽",
            "1000 | не больше 1000 ₽"
    })
    @DisplayName("подсказка при неверной сумме на кино не пишет «от X до X»")
    void outOfRangeHint(int total, String expected) {
        assertThat(screens(null).cinemaOutOfRange(total))
                .contains(expected)
                .doesNotContainPattern("от (\\d+) до \\1 ");
    }

    @Test
    @DisplayName("финальное сообщение отправляет в мини-приложение, а не пересказывает суммы")
    void readySendsToTheApp() {
        assertThat(screens(null).ready())
                .contains("мини-приложении")
                .doesNotContain("Записал");
    }

    @Test
    @DisplayName("в текстах бота не осталось разговора про удалённое")
    void textsDoNotPromiseRemovedFeatures() {
        Screens s = screens("test_bot");
        String all = s.welcome() + s.askCinema(3000) + s.cinemaOutOfRange(5000) + s.ready();
        assertThat(all)
                .doesNotContain("/menu")
                .doesNotContain("/help")
                .doesNotContain("👎")
                .doesNotContain("Кнопки под карточкой")
                .doesNotContain("откуда поедешь");
    }
}
