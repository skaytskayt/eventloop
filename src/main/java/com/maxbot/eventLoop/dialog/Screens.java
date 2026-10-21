package com.maxbot.eventLoop.dialog;

import com.maxbot.eventLoop.config.AppProperties;
import com.maxbot.eventLoop.geo.Haversine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.MessageButton;
import ru.max.botapi.model.OpenAppButton;
import ru.max.botapi.model.RequestGeoLocationButton;

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

    /** Точка отсчёта расстояния: откуда человек поедет. */
    public record Origin(String name, double lat, double lon) {
    }

    /**
     * Точки, которые можно выбрать кнопкой. Mini App знает их названия из
     * своего списка ORIGINS в app.js: payload не несёт кириллицу, поэтому
     * название восстанавливается по координатам. Держать в паре с app.js —
     * синхронность проверяет ScreensMiniAppTest.
     */
    public static final List<Origin> ORIGINS = List.of(
            new Origin("Москва · центр", 55.7558, 37.6173),
            new Origin("Москва · м. ВДНХ", 55.8196, 37.6408),
            new Origin("Москва · м. Юго-Западная", 55.6631, 37.4829),
            new Origin("Москва · м. Выхино", 55.7163, 37.8164),
            new Origin("Казань · центр", 55.7961, 49.1064),
            new Origin("Казань · м. Козья слобода", 55.8182, 49.0999),
            new Origin("Казань · м. Горки", 55.7600, 49.1916),
            new Origin("Казань · м. Проспект Победы", 55.7504, 49.2096));

    public static final Origin MOSCOW_CENTER = ORIGINS.get(0);
    public static final Origin KAZAN_CENTER = ORIGINS.get(4);
    public static final String SKIP = "Пропустить";
    public static final String GEO_NAME = "По геолокации";

    /**
     * Payload кнопки статистики: Mini App с ним открывается сразу на экране
     * статистики. Держать в паре с STATS_PARAM в app.js — синхронность
     * проверяет ScreensMiniAppTest.
     */
    public static final String STATS_PAYLOAD = "stats";

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
                1. Скажешь, сколько всего на карте и сколько из этого на кино, — и ещё спрошу, откуда считать расстояние.
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
     * Третий и последний вопрос. Город подборки определяется по точке, поэтому
     * казанец, выбравший Казань, видит казанскую афишу без отдельного вопроса.
     */
    public String askOrigin() {
        return """
                И последнее: **откуда считать расстояние?** Отправь геолокацию или выбери точку кнопкой — по ней же выберу город подборки, Москву или Казань.

                «Пропустить» — буду считать от центра Москвы.""";
    }

    /**
     * Геолокация первой строкой — точнее любой кнопки. Точки — кнопками типа
     * message: нажатие присылает их текст обычным сообщением, и обрабатывать
     * callback не нужно.
     */
    public List<List<Button>> originKeyboard() {
        List<List<Button>> rows = new ArrayList<>();
        rows.add(List.of(new RequestGeoLocationButton("📍 Отправить геолокацию", null)));
        for (int i = 0; i < ORIGINS.size(); i += 2) {
            List<Button> row = new ArrayList<>();
            for (Origin origin : ORIGINS.subList(i, Math.min(i + 2, ORIGINS.size()))) {
                row.add(new MessageButton(origin.name()));
            }
            rows.add(row);
        }
        rows.add(List.of(new MessageButton(SKIP)));
        return rows;
    }

    /** Точка по тексту кнопки; просто «Москва» или «Казань» — центр города. */
    public Optional<Origin> originByText(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.equals("москва")) {
            return Optional.of(MOSCOW_CENTER);
        }
        if (t.equals("казань")) {
            return Optional.of(KAZAN_CENTER);
        }
        return ORIGINS.stream().filter(o -> o.name().toLowerCase(Locale.ROOT).equals(t)).findFirst();
    }

    /** Ближайший из двух центров — по нему Mini App выбирает город ленты. */
    public Origin nearestCenter(double lat, double lon) {
        return List.of(MOSCOW_CENTER, KAZAN_CENTER).stream()
                .min(Comparator.comparingDouble(c -> Haversine.distanceKm(lat, lon, c.lat(), c.lon())))
                .orElseThrow();
    }

    /** Дописка к {@link #ready()}: от чего считается расстояние и как это сменить. */
    public String originNote(String originName) {
        if (originName == null) {
            return "\n\nРасстояние считаю от центра Москвы.";
        }
        return "\n\nРасстояние считаю от: **%s**. Сменить — пришли геолокацию или нажми другую точку в сообщении выше."
                .formatted(originName);
    }

    /**
     * Кнопка, открывающая мини-приложение бота внутри MAX. Обе суммы уезжают
     * в payload: своего онбординга у приложения нет, и без них оно считало бы
     * от номинала. Формат {@code <кино>_<остальное>} — MAX пропускает в payload
     * только латиницу, цифры, «_» и «-».
     *
     * <p>С точкой отсчёта payload расширяется до
     * {@code <кино>_<остальное>_<lat*10000>_<lon*10000>}: целые со знаком,
     * без точки и кириллицы, не длиннее 64 символов. Mini App понимает оба
     * вида, поэтому старые кнопки в истории чата продолжают работать.
     */
    public List<List<Button>> miniAppKeyboard(int cinemaRub, int otherRub) {
        return miniAppKeyboard(cinemaRub, otherRub, null, null);
    }

    public List<List<Button>> miniAppKeyboard(int cinemaRub, int otherRub, Double lat, Double lon) {
        String bot = properties.botUsername();
        if (bot == null || bot.isBlank()) {
            return null;
        }
        return List.of(List.of(new OpenAppButton("🎭 Открыть подборку", bot, null,
                miniAppPayload(cinemaRub, otherRub, lat, lon))));
    }

    public String miniAppPayload(int cinemaRub, int otherRub) {
        return miniAppPayload(cinemaRub, otherRub, null, null);
    }

    public String miniAppPayload(int cinemaRub, int otherRub, Double lat, Double lon) {
        String wallets = cinemaRub + "_" + otherRub;
        if (lat == null || lon == null) {
            return wallets;
        }
        return wallets + "_" + Math.round(lat * 10000) + "_" + Math.round(lon * 10000);
    }

    /** Что дописать, когда username бота не настроен. */
    public String miniAppMissing() {
        return "\n\n_Кнопка приложения пока не настроена — задайте MAX\\_BOT\\_USERNAME._";
    }

    /** Ответ администратору на /stats. */
    public String statsTitle() {
        return "**Статистика EventLoop**\n"
                + "Воронка, покупки и последние действия — в мини-приложении, "
                + "обновляются каждые 3 секунды.";
    }

    /** Кнопка экрана статистики; null, если username бота не задан. */
    public List<List<Button>> statsKeyboard() {
        String bot = properties.botUsername();
        if (bot == null || bot.isBlank()) {
            return null;
        }
        return List.of(List.of(new OpenAppButton("📊 Открыть статистику", bot, null, STATS_PAYLOAD)));
    }

    /** Ответ на /stats не администратору: заодно это способ узнать свой id. */
    public String statsForbidden(long maxUserId) {
        return String.format(Locale.ROOT,
                "Команда только для администраторов. Ваш MAX id: %d — добавьте его в ADMIN\\_MAX\\_IDS.",
                maxUserId);
    }

    /** Что дописать к statsTitle, когда username бота не настроен. */
    public String statsMissing() {
        return "\n\n_Кнопки нет: не задан username бота — задайте MAX\\_BOT\\_USERNAME._";
    }
}
