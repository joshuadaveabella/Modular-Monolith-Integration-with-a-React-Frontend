package edu.cit.abella.channel;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// One row per feed event FULLY handled (our own side effect done AND
// Tiangge successfully notified of the outcome where applicable). An event
// only lands here once both halves succeed - see FeedPoller.
@Entity
@Table(name = "channel_processed_events")
class ChannelProcessedEventEntity {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(nullable = false)
    private Long seq;

    @Column(nullable = false)
    private String type;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected ChannelProcessedEventEntity() {
    }

    ChannelProcessedEventEntity(String eventId, Long seq, String type) {
        this.eventId = eventId;
        this.seq = seq;
        this.type = type;
    }

    @PrePersist
    protected void onCreate() {
        this.processedAt = LocalDateTime.now();
    }

    String getEventId() { return eventId; }
    Long getSeq() { return seq; }
}
