package com.maxbot.eventLoop.dialog;

import com.maxbot.eventLoop.config.AppProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.LinkButton;

/**
 * Тексты и кнопки бота.
 *
 * <p>Бот делает ровно одно: узнаёт, сколько на карте, и отдаёт человека
 * мини-приложению. Лента, свайпы и фильтры живут там — в переписке карточка
 * листается втрое медленнее, а две реализации одной ленты расходятся на
 * второй же правке.
 */
@Component
public class Screens {

    private final AppProperties properties;

    public Screens(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * Первое сообщение. Объясняет, что дальше будет, и сразу задаёт первый
     * вопрос: экран без вопроса оставляет человека гадать, чего от него ждут.
     */
    public String welcome() {
        return """
                Привет! Помогу потратить Пушкинскую карту на то, куда реально дойдёшь.

                **Как это работает**
                1. Скажешь, сколько на карте — отдельно на кино и отдельно на остальное.
                2. Открою подборку мероприятий: листаешь карточки, нравится — вправо, нет — влево.
                3. Под каждой карточкой видно цену, дату, площадку и сколько останется на карте, если пойти именно сюда.
                4. Понравилось — жмёшь «Купить билет», попадаешь на страницу мероприятия. Вернёшься — спрошу, купил ли, и спишу с нужного кошелька.

                Там же фильтры: дата, категория и сколько километров готов проехать.

                Начнём с денег. **Сколько на карте на кино?** Напиши цифрами, например %d."""
                .formatted(properties.cinemaLimitRub());
    }

    public String askCinema() {
        return "**Сколько на карте на кино?**\n\nНапиши цифрами, например %d. Это отдельный кошелёк, до %d ₽."
                .formatted(properties.cinemaLimitRub(), properties.cinemaLimitRub());
    }

    /** Второй вопрос. Первую сумму повторяем: её уже не видно за клавиатурой. */
    public String askOther(int cinemaRub) {
        return """
                Кино: **%d ₽**.

                **А сколько осталось на остальное?** Театры, музеи, концерты, экскурсии — это второй кошелёк, до %d ₽."""
                .formatted(cinemaRub, properties.otherLimitRub());
    }

    /** Обе суммы названы — дальше человек уходит в мини-приложение. */
    public String ready(int cinemaRub, int otherRub) {
        return """
                Записал: **%d ₽** на кино и **%d ₽** на остальное.

                Дальше — в приложении. Там подборка мероприятий карточками, фильтры по дате, категории и расстоянию, а в профиле видно оба кошелька и всё, что уже куплено."""
                .formatted(cinemaRub, otherRub);
    }

    /**
     * Кнопка-ссылка на мини-приложение. Обе суммы уезжают параметрами: своего
     * онбординга у приложения нет, и без них оно считало бы от номинала.
     */
    public List<List<Button>> miniAppKeyboard(int cinemaRub, int otherRub) {
        String url = miniAppUrl(cinemaRub, otherRub);
        if (url == null) {
            return null;
        }
        return List.of(List.of(new LinkButton("🎭 Открыть подборку", url)));
    }

    /** {@code null}, если адрес приложения не задан в конфигурации. */
    public String miniAppUrl(int cinemaRub, int otherRub) {
        String base = properties.miniappUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return base + (base.contains("?") ? "&" : "?")
                + "cinema=" + cinemaRub + "&other=" + otherRub;
    }

    /** Что дописать, когда адрес приложения не настроен. */
    public String miniAppMissing() {
        return "\n\n_Ссылка на приложение пока не настроена — задайте MINIAPP\\_URL._";
    }
}
