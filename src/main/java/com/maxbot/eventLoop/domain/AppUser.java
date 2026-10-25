package com.maxbot.eventLoop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Пользователь бота.
 *
 * <p>Бот спрашивает, сколько всего на карте и сколько из этого на кино, и
 * хранит два кошелька — кино и остальное, состояние диалога и точку отсчёта
 * расстояния (колонки origin_* живут в схеме с V1). Район, максимальное
 * расстояние, категории и фильтр по датам по-прежнему в мини-приложении:
 * колонки под них в схеме остались, но не отображаются — Hibernate с
 * {@code ddl-auto: validate} лишние колонки не проверяет.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "max_user_id", nullable = false, unique = true)
    private long maxUserId;

    @Column(name = "chat_id")
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dialog_state", nullable = false, length = 32)
    private DialogState dialogState = DialogState.NEW;

    @Column(name = "cinema_balance_rub", nullable = false)
    private int cinemaRub;

    @Column(name = "other_balance_rub", nullable = false)
    private int otherRub;

    /** Точка отсчёта расстояния; все три null — считать от центра Москвы. */
    @Column(name = "origin_name", length = 128)
    private String originName;

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lon")
    private Double originLon;

    /** Последнее «Заполнить анкету»; null — анкету заново не начинали. */
    @Column(name = "form_at")
    private Instant formAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AppUser() {
    }

    public AppUser(long maxUserId, Long chatId) {
        this.maxUserId = maxUserId;
        this.chatId = chatId;
    }

    public Long getId() {
        return id;
    }

    public long getMaxUserId() {
        return maxUserId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public DialogState getDialogState() {
        return dialogState;
    }

    public void setDialogState(DialogState dialogState) {
        this.dialogState = dialogState;
        this.updatedAt = Instant.now();
    }

    public int getCinemaRub() {
        return cinemaRub;
    }

    public void setCinemaRub(int cinemaRub) {
        this.cinemaRub = cinemaRub;
        this.updatedAt = Instant.now();
    }

    public int getOtherRub() {
        return otherRub;
    }

    public void setOtherRub(int otherRub) {
        this.otherRub = otherRub;
        this.updatedAt = Instant.now();
    }

    public String getOriginName() {
        return originName;
    }

    public Double getOriginLat() {
        return originLat;
    }

    public Double getOriginLon() {
        return originLon;
    }

    public Instant getFormAt() {
        return formAt;
    }

    public void setFormAt(Instant formAt) {
        this.formAt = formAt;
    }

    public void setOrigin(String name, Double lat, Double lon) {
        this.originName = name;
        this.originLat = lat;
        this.originLon = lon;
        this.updatedAt = Instant.now();
    }
}
