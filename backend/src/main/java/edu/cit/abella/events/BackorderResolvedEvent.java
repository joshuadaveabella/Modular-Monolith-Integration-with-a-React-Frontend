package edu.cit.abella.events;

// Published by OrderService when a BACKORDERED order is resolved (Task 6).
// resolvedStatus is one of our own order statuses - "CONFIRMED" or
// "CANCELLED" - never a Tiangge-specific term. This is what lets channel's
// BackorderNotifier tell Tiangge the outcome without OrderService ever
// importing anything from the channel package.
public class BackorderResolvedEvent {

    private final Long orderId;
    private final String resolvedStatus; // "CONFIRMED" or "CANCELLED"

    public BackorderResolvedEvent(Long orderId, String resolvedStatus) {
        this.orderId = orderId;
        this.resolvedStatus = resolvedStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getResolvedStatus() {
        return resolvedStatus;
    }
}
