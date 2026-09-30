package com.maxbot.eventLoop.dialog;

import com.maxbot.eventLoop.config.AppProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.OpenAppButton;

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
                1. Скажешь, сколько всего на карте и сколько из этого на кино.
                2. Открою подборку мероприятий: листаешь карточки, нравится — вправо, нет — влево.
                3. Под каждой карточкой видно цену, дату, площадку и сколько останется на карте, если пойти именно сюда.
                4. Понравилось — жмёшь «Купить билет», попадаешь на страницу мероприятия. Вернёшься — спрошу, купил ли, и спишу с нужного кошелька.

                Там же фильтры: дата, категория и сколько километров готов проехать.

                Начнём с денег. **Сколько всего на карте?** Напиши цифрами, например %d."""
                .formatted(properties.nominalRub());
    }

    /**
     * Второй вопрос. Общую сумму повторяем: её уже не видно за клавиатурой.
     */
    public String askCinema(int totalRub) {
        return """
                Всего на карте: **%d ₽**.

                **Сколько из них на кино?** Кино — отдельный кошелёк. Напиши сумму до %d ₽, остальное посчитаю сам."""
                .formatted(totalRub, maxCinema(totalRub));
    }

    /** Наименьшая сумма на кино, при которой «остальное» помещается в свой кошелёк. */
    public int minCinema(int totalRub) {
        return Math.max(0, totalRub - properties.otherLimitRub());
    }

    /** Больше номинала кошелька и больше всей карты на кино быть не может. */
    public int maxCinema(int totalRub) {
        return Math.min(properties.cinemaLimitRub(), totalRub);
    }

    /**
     * Сумма на кино не сходится с общей: объясняем, в каких пределах она может
     * быть. Когда подходит ровно одна сумма, называем её, а не «от X до X».
     */
    public String cinemaOutOfRange(int totalRub) {
        int min = minCinema(totalRub);
        int max = maxCinema(totalRub);
        String range = min == max ? "только %d ₽".formatted(max)
                : min == 0 ? "не больше %d ₽".formatted(max)
                : "от %d до %d ₽".formatted(min, max);
        return "При %d ₽ на карте на кино может быть %s: на кино — до %d ₽, на остальное — до %d ₽. Сколько на кино?"
                .formatted(totalRub, range, properties.cinemaLimitRub(), properties.otherLimitRub());
    }

    /** Обе суммы известны — дальше всё делается в мини-приложении. */
    public String ready() {
        return """
                Готово! Дальше всё делается в мини-приложении: подборка мероприятий карточками, фильтры по дате, категории и расстоянию, а в профиле — оба кошелька и всё, что уже куплено.""";
    }

    /**
     * Кнопка, открывающая мини-приложение бота внутри MAX. Обе суммы уезжают
     * в payload: своего онбординга у приложения нет, и без них оно считало бы
     * от номинала. Формат {@code <кино>_<остальное>} — MAX пропускает в payload
     * только латиницу, цифры, «_» и «-».
     */
    public List<List<Button>> miniAppKeyboard(int cinemaRub, int otherRub) {
        String bot = properties.botUsername();
        if (bot == null || bot.isBlank()) {
            return null;
        }
        return List.of(List.of(new OpenAppButton("🎭 Открыть подборку", bot, null,
                miniAppPayload(cinemaRub, otherRub))));
    }

    public String miniAppPayload(int cinemaRub, int otherRub) {
        return cinemaRub + "_" + otherRub;
    }

    /** Что дописать, когда username бота не настроен. */
    public String miniAppMissing() {
        return "\n\n_Кнопка приложения пока не настроена — задайте MAX\\_BOT\\_USERNAME._";
    }
}
