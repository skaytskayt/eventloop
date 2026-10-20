package com.maxbot.eventLoop.miniapp;

/**
 * Действие пользователя в Mini App и то, как оно меняет сохранённую реакцию.
 *
 * <p>Просмотр, переход к покупке и «ещё нет» только пишутся в журнал — они
 * нужны воронке, но состояния ленты не меняют.
 */
public enum MiniAppAction {
    VIEW(null, false),
    LIKE("LIKED", false),
    SAVE("SAVED", false),
    DISLIKE("DISLIKED", false),
    UNDO(null, true),
    CLICK_BUY(null, false),
    PURCHASE_YES("BOUGHT", false),
    PURCHASE_NO(null, false),
    // Клиент возвращает отменённую покупку в «Пойду».
    PURCHASE_CANCEL("LIKED", false);

    private final String reactionKind;
    private final boolean clearsReaction;

    MiniAppAction(String reactionKind, boolean clearsReaction) {
        this.reactionKind = reactionKind;
        this.clearsReaction = clearsReaction;
    }

    /** Во что превращается реакция; null — не меняется. */
    public String reactionKind() {
        return reactionKind;
    }

    public boolean clearsReaction() {
        return clearsReaction;
    }
}
