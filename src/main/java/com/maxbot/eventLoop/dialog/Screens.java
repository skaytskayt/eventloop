package com.maxbot.eventLoop.dialog;

import com.maxbot.eventLoop.config.AppProperties;
import com.maxbot.eventLoop.geo.Haversine;
import java.util.Comparator;
import java.util.List;
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
     * Центры городов. По ближайшему из них выбирается город ленты; те же
     * координаты лежат в CITIES в app.js — синхронность проверяет
     * ScreensMiniAppTest.
     */
    public static final Origin MOSCOW_CENTER = new Origin("Москва · центр", 55.7558, 37.6173);
    public static final Origin KAZAN_CENTER = new Origin("Казань · центр", 55.7961, 49.1064);
    public static final String GEO_NAME = "По геолокации";

    /**
     * Payload кнопки статистики: Mini App с ним открывается сразу на экране
     * статистики. Держать в паре с STATS_PARAM в app.js — синхронность
     * проверяет ScreensMiniAppTest.
     */
    public static final String STATS_PAYLOAD = "stats";

    /** Кнопка под приветствием: MAX присылает её текст сообщением, по нему анкета и начинается. */
    public static final String FILL_FORM = "📝 Заполнить анкету";

    private final AppProperties properties;

    public Screens(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * Первое сообщение. Объясняет, что дальше будет, и зовёт к кнопке анкеты:
     * /start ничего не сбрасывает, поэтому вопрос задаётся только по кнопке.
     */
    public String welcome() {
        return """
                Привет! Помогу найти интересные мероприятия и потратить Пушкинскую карту на то, куда действительно захочется сходить.

                **Как это работает**
                1. Укажешь баланс карты, сумму на кино и адрес, откуда считать расстояние.
                2. Переходи в мини-приложение и листай карточки: вправо — хочешь сходить уже сейчас, влево — не твоё, «Отложить» — сохранить на потом.
                3. Смотри цену, дату и место, а понравилось — переходи за билетом.
                4. Фильтруй события по дате, категории и расстоянию.

                **Хочешь потратить весь остаток?** Выбери в мини-приложении режим «Потратить весь баланс» — собирай подборку и заменяй отдельные карточки или все сразу.

                Нажми «Заполнить анкету», чтобы начать. Если анкета уже заполнена, просто открывай подборку — всё сохранено.""";
    }

    /** Под приветствием — кнопка анкеты и, если анкета пройдена, кнопка подборки. */
    public List<List<Button>> welcomeKeyboard(List<List<Button>> miniApp) {
        List<List<Button>> rows = new java.util.ArrayList<>();
        rows.add(List.of(new MessageButton(FILL_FORM)));
        if (miniApp != null) {
            rows.addAll(miniApp);
        }
        return rows;
    }

    /** Первый вопрос анкеты. */
    public String askTotal() {
        return "Начнём! **Сколько всего денег на карте?** Напиши цифрами, например %d."
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
        return "При %d ₽ на карте на кино может быть %s. Сколько на кино?"
                .formatted(totalRub, range);
    }

    /** Обе суммы известны — дальше всё делается в мини-приложении. */
    public String ready() {
        return """
                Готово! Дальше всё делается в мини-приложении: подборка мероприятий карточками, фильтры по дате, категории и расстоянию, а в профиле — оба кошелька и всё, что уже куплено.""";
    }

    /**
     * Третий и последний вопрос: своя точка — геолокацией или адресом текстом.
     * Город подборки определяется по ней, поэтому казанец видит казанскую
     * афишу без отдельного вопроса.
     */
    public String askOrigin() {
        return "📍 Откуда поедешь? Поделись геолокацией кнопкой ниже или напиши адрес — "
                + "например, «Казань, Баумана 10» или просто «Москва, Арбат».";
    }

    /** Единственная кнопка — геолокация; адрес человек пишет обычным сообщением. */
    public List<List<Button>> originKeyboard() {
        return List.of(List.of(new RequestGeoLocationButton("📍 Отправить геолокацию", null)));
    }

    /** Адрес найден: показываем, что именно, — первое совпадение может быть не тем. */
    public String addressFound(String name) {
        return "Нашёл: %s. Считаю расстояние отсюда.".formatted(name);
    }

    /** Адрес не найден, далеко от обоих городов или геокодер не ответил. */
    public String addressNotFound() {
        return "Не нашёл такой адрес в Москве или Казани. Попробуй точнее — улица и дом — или отправь геолокацию.";
    }

    /** Адрес или город дальше 100 км от Москвы и Казани. */
    public String outsidePilot() {
        return "Пока это пилотный проект: он работает только в Москве и Казани. "
                + "Но мы обязательно планируем расширяться! А пока напиши адрес в Москве или Казани — "
                + "или отправь геолокацию.";
    }

    /** Найден только город: расстояние от центра врало бы — просим точнее. */
    public String addressTooBroad(String name) {
        return "%s — это весь город. Напиши точнее: улицу и дом или станцию метро — или отправь геолокацию."
                .formatted(name);
    }

    /** /address до того, как назван баланс: анкета идёт по порядку. */
    public String addressTooEarly() {
        return "Сначала скажи, сколько на карте, — потом спрошу, откуда поедешь.";
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
            return "\n\nРасстояние считаю от центра Москвы. Указать свой адрес — /address.";
        }
        return "\n\nРасстояние считаю от: **%s**. Сменить адрес — /address."
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

    /** Подпись к /stats в демо-режиме, когда статистика открыта всем. */
    public String statsDemoNote() {
        return "\n\n_Демо: статистика открыта всем, чтобы показать экспертам аналитику проекта. "
                + "В проде доступ будет только у администратора._";
    }

    /** Кнопка экрана статистики; null, если username бота не задан. */
    public List<List<Button>> statsKeyboard() {
        String bot = properties.botUsername();
        if (bot == null || bot.isBlank()) {
            return null;
        }
        return List.of(List.of(new OpenAppButton("📊 Открыть статистику", bot, null, STATS_PAYLOAD)));
    }

    /** Ответ на /stats не администратору: команда для него не существует. */
    public String unknownCommand() {
        return "Такой команды нет.";
    }

    /** Что дописать к statsTitle, когда username бота не настроен. */
    public String statsMissing() {
        return "\n\n_Кнопки нет: не задан username бота — задайте MAX\\_BOT\\_USERNAME._";
    }
}
