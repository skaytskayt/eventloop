package com.maxbot.eventLoop.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxbot.eventLoop.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.LinkButton;

/**
 * Ссылка на мини-приложение — единственное, ради чего теперь существует бот.
 * Если она собрана неверно, человек попадает в приложение с чужими суммами
 * или не попадает вовсе, а в чате это выглядит как «кнопка не работает».
 */
class ScreensMiniAppTest {

    private static Screens screens(String url) {
        return new Screens(new AppProperties(2000, 3000, url));
    }

    @ParameterizedTest(name = "{0} → {3}")
    @CsvSource({
            "https://app.example/,           1200, 2500, https://app.example/?cinema=1200&other=2500",
            "https://app.example/index.html, 0,    0,    https://app.example/index.html?cinema=0&other=0",
            // У адреса уже есть параметры — дописываем через &, а не через ?.
            "https://app.example/?v=2,       2000, 3000, https://app.example/?v=2&cinema=2000&other=3000"
    })
    @DisplayName("обе суммы дописываются к адресу приложения")
    void appendsBothWallets(String base, int cinema, int other, String expected) {
        assertThat(screens(base).miniAppUrl(cinema, other)).isEqualTo(expected);
    }

    @Test
    @DisplayName("без настроенного адреса ссылки нет, и кнопки тоже")
    void noUrlNoButton() {
        assertThat(screens(null).miniAppUrl(1000, 1000)).isNull();
        assertThat(screens("").miniAppUrl(1000, 1000)).isNull();
        assertThat(screens(null).miniAppKeyboard(1000, 1000)).isNull();
    }

    @Test
    @DisplayName("кнопка — ссылка на приложение с обеими суммами")
    void keyboardCarriesTheLink() {
        List<List<Button>> keyboard = screens("https://app.example/").miniAppKeyboard(500, 1500);
        assertThat(keyboard).hasSize(1);
        assertThat(keyboard.getFirst()).hasSize(1);
        assertThat(keyboard.getFirst().getFirst())
                .isInstanceOf(LinkButton.class)
                .extracting(b -> ((LinkButton) b).url())
                .isEqualTo("https://app.example/?cinema=500&other=1500");
    }

    /**
     * Приветствие — единственное место, где человек узнаёт, что его ждёт.
     * Без механики оно превращается в «напиши число» без объяснения зачем.
     */
    @Test
    @DisplayName("приветствие объясняет механику и задаёт первый вопрос")
    void welcomeExplainsHowItWorks() {
        String welcome = screens("https://app.example/").welcome();
        assertThat(welcome)
                .contains("подборку")
                .contains("вправо")
                .contains("останется на карте")
                .contains("Купить билет")
                .contains("фильтры")
                .contains("на кино");
    }

    @Test
    @DisplayName("второй вопрос повторяет уже названную сумму")
    void secondQuestionRepeatsTheFirstAmount() {
        assertThat(screens(null).askOther(1200))
                .contains("1200")
                .contains("остальное");
    }

    @Test
    @DisplayName("в текстах бота не осталось разговора про удалённое")
    void textsDoNotPromiseRemovedFeatures() {
        Screens s = screens("https://app.example/");
        String all = s.welcome() + s.askCinema() + s.askOther(100) + s.ready(100, 200);
        assertThat(all)
                .doesNotContain("/menu")
                .doesNotContain("/help")
                .doesNotContain("👎")
                .doesNotContain("Кнопки под карточкой")
                .doesNotContain("откуда поедешь");
        assertThat(s.ready(100, 200)).contains("100").contains("200");
    }
}
