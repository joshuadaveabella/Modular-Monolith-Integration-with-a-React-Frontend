package edu.cit.abella.events;

public class OrderRejectedEvent {

    private final Long orderId; // may be null if nothing was persisted
    private final String reason;

    public OrderRejectedEvent(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }
}
