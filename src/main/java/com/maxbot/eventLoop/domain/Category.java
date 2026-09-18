package com.maxbot.eventLoop.domain;

/** Направления программы Пушкинской карты, представленные в датасете. */
public enum Category {

    THEATRE("Театр", "🎭"),
    CINEMA("Кино", "🎬"),
    MUSEUM("Музеи и выставки", "🖼"),
    CONCERT("Концерты", "🎼"),
    EXCURSION("Экскурсии и лекции", "🚶");

    private final String title;
    private final String emoji;

    Category(String title, String emoji) {
        this.title = title;
        this.emoji = emoji;
    }

    public String title() {
        return title;
    }

    public String emoji() {
        return emoji;
    }

    public String label() {
        return emoji + " " + title;
    }

    /** Кино оплачивается из отдельного подлимита внутри общего номинала. */
    public boolean hasCinemaSublimit() {
        return this == CINEMA;
    }
}
