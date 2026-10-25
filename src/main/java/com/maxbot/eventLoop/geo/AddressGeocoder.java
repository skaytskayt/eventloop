package com.maxbot.eventLoop.geo;

import java.util.Optional;

/**
 * Поиск координат по адресу, написанному человеком.
 *
 * <p>Интерфейс, а не сразу класс: тесты диалога подменяют геокодер фейком
 * и проверяют ветки «найдено», «не найдено», «далеко» и «сбой» без сети.
 */
public interface AddressGeocoder {

    /**
     * Найденное место. {@code name} — короткий адрес не длиннее 128 символов
     * (колонка origin_name) и без символов * _ ` [ ]: имя уходит в
     * MARKDOWN-сообщение бота. {@code coarse} — найден целый город или
     * регион, а не улица: расстояние от его центра врало бы, поэтому такой
     * ответ переспрашивают.
     */
    record Place(String name, double lat, double lon, boolean coarse) {

        public Place(String name, double lat, double lon) {
            this(name, lat, lon, false);
        }
    }

    /** Пусто — не найдено или сбой; исключения наружу по контракту не летят. */
    Optional<Place> geocode(String query);
}
