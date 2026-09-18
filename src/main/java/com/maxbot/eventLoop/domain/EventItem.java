package com.maxbot.eventLoop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_item")
public class EventItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_key", nullable = false, unique = true)
    private String externalKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(name = "age_rating", nullable = false, length = 8)
    private String ageRating;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "poster_url", nullable = false)
    private String posterUrl;

    @Column(name = "event_url", nullable = false)
    private String eventUrl;

    @Column(name = "ticket_url", nullable = false)
    private String ticketUrl;

    protected EventItem() {
    }

    public Long getId() {
        return id;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public Category getCategory() {
        return category;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public Venue getVenue() {
        return venue;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getEventUrl() {
        return eventUrl;
    }

    public String getTicketUrl() {
        return ticketUrl;
    }
}
