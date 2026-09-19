package com.maxbot.eventLoop.importer;

import com.maxbot.eventLoop.domain.Venue;
import com.maxbot.eventLoop.repo.VenueRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Сопоставляет название площадки из источника с площадкой в нашей базе.
 *
 * <p>Координат источник не отдаёт — страницы площадок закрыты антибот-защитой.
 * Поэтому координаты берутся только из известных нам площадок, а мероприятия
 * в незнакомых местах не импортируются: показать карточку с неверным
 * расстоянием хуже, чем не показать её вовсе. Пропущенные площадки попадают
 * в отчёт импорта, чтобы их добавляли осознанно, а не молча теряли.
 */
@Component
public class VenueResolver {

    /**
     * Слова, которые есть у половины московских площадок и потому ничего
     * не различают. Без их отсева «Театр Гоголя» совпал бы с «Театром сатиры».
     */
    private static final Set<String> STOPWORDS = Set.of(
            "театр", "театра", "театре", "имени", "им", "государственный",
            "государственная", "московский", "московская", "академический",
            "академическая", "центральный", "центр", "сцена", "сцены", "новая",
            "малая", "большая", "музей", "музея", "зал", "дворец", "российской",
            "российский", "квартира", "арт", "кафе", "и", "на", "пр", "п",
            "экскурсии", "холл", "арена", "студия", "современного", "искусства");

    /**
     * Сокращения и обиходные названия, которые никаким сравнением строк
     * не свести к официальным.
     */
    private static final Map<String, String> ALIASES = Map.of(
            "рамт", "российский академический молодёжный театр",
            "мхт", "мхт имени а. п. чехова",
            "гкд", "государственный кремлевский дворец",
            "мамт", "музыкальный театр имени станиславского и немировича-данченко");

    private final VenueRepository venues;

    public VenueResolver(VenueRepository venues) {
        this.venues = venues;
    }

    public Optional<Venue> resolve(String sourceName) {
        if (sourceName == null || sourceName.isBlank()) {
            return Optional.empty();
        }
        String expanded = ALIASES.getOrDefault(
                sourceName.trim().toLowerCase(Locale.ROOT), sourceName);

        List<Venue> all = venues.findAllByOrderByNameAsc();
        String needle = normalize(expanded);

        for (Venue v : all) {
            if (normalize(v.getName()).equals(needle)) {
                return Optional.of(v);
            }
        }
        // «Симоновская сцена Театра имени Евгения Вахтангова» должна найти Вахтангова:
        // совпадения по различающему слову достаточно, служебные слова отброшены.
        Set<String> needleTokens = significantTokens(expanded);
        if (needleTokens.isEmpty()) {
            return Optional.empty();
        }
        for (Venue v : all) {
            // У нашей площадки набор берётся шире: иначе «Театр имени Сац»,
            // у которого различающее слово короткое, не нашёл бы «Детский
            // музыкальный театр имени Н. И. Сац», где есть и длинные слова.
            Set<String> tokens = collect(v.getName(), 3);
            if (tokens.stream().anyMatch(needleTokens::contains)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    /**
     * Слова, по которым площадку действительно можно опознать.
     *
     * <p>Короткие отбрасываются: «на», «пр» совпадают у всего подряд.
     */
    static Set<String> significantTokens(String raw) {
        Set<String> tokens = collect(raw, 5);
        // У коротких имён собственных различающих слов иначе не остаётся вовсе:
        // «Театр имени Сац» — это «театр» и «имени» (служебные) плюс «сац».
        return tokens.isEmpty() ? collect(raw, 3) : tokens;
    }

    static Set<String> collect(String raw, int minLength) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String word : split(raw)) {
            // Голое число площадку не опознаёт: «Сцена 2» и «Зал 2» — разные места.
            boolean numeric = word.chars().allMatch(Character::isDigit);
            if (word.length() >= minLength && !numeric && !STOPWORDS.contains(word)) {
                tokens.add(stem(word));
            }
        }
        return tokens;
    }

    /**
     * Грубое усечение окончания: «вахтангова» и «вахтанговский» должны
     * считаться одним словом, а полноценная морфология здесь избыточна.
     */
    private static String stem(String word) {
        return word.length() > 6 ? word.substring(0, word.length() - 2) : word;
    }

    private static String[] split(String raw) {
        // \\b в Java по умолчанию опирается на ASCII-класс \\w и на кириллице
        // не срабатывает вовсе — поэтому режем по явному набору символов.
        return normalizeSpaced(raw).split("\\s+");
    }

    static String normalize(String raw) {
        return normalizeSpaced(raw).replace(" ", "");
    }

    private static String normalizeSpaced(String raw) {
        return raw.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^а-я0-9]+", " ")
                .trim();
    }

    /** Для отчётов и тестов: какие слова сочтены различающими. */
    public static List<String> debugTokens(String raw) {
        return Arrays.stream(split(raw)).filter(w -> w.length() >= 5
                && !STOPWORDS.contains(w)).toList();
    }
}
