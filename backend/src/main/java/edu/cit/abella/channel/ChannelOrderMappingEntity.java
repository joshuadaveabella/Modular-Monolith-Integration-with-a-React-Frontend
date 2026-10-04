package edu.cit.abella.channel;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// "Each Tiangge order must become exactly one order in your system." This
// row is the proof: before creating a new internal order for an
// ORDER_PLACED event, FeedPoller checks whether this tiangeOrderId already
// has a mapping. If it does, it re-sends the existing decision instead of
// creating a second order - this holds even if the eventId-based dedup in
// ChannelProcessedEventEntity somehow didn't catch a re-delivery.
@Entity
@Table(name = "channel_order_mapping")
class ChannelOrderMappingEntity {

    @Id
    @Column(name = "tiangge_order_id")
    private String tiangeOrderId;

    @Column(name = "shop_order_id", nullable = false)
    private Long shopOrderId;

    @Column(name = "decision_confirmed", nullable = false)
    private boolean decisionConfirmed;

    @Column(name = "cancellation_confirmed", nullable = false)
    private boolean cancellationConfirmed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ChannelOrderMappingEntity() {
    }

    ChannelOrderMappingEntity(String tiangeOrderId, Long shopOrderId) {
        this.tiangeOrderId = tiangeOrderId;
        this.shopOrderId = shopOrderId;
        this.decisionConfirmed = false;
        this.cancellationConfirmed = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    String getTiangeOrderId() { return tiangeOrderId; }
    Long getShopOrderId() { return shopOrderId; }
    boolean isDecisionConfirmed() { return decisionConfirmed; }
    void setDecisionConfirmed(boolean decisionConfirmed) { this.decisionConfirmed = decisionConfirmed; }
    boolean isCancellationConfirmed() { return cancellationConfirmed; }
    void setCancellationConfirmed(boolean cancellationConfirmed) { this.cancellationConfirmed = cancellationConfirmed; }
}
