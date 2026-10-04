package edu.cit.abella.events;

import java.util.List;

public class OrderPlacedEvent {

    private final Long orderId;
    private final List<String> productIds;
    private final int totalQuantity;

    public OrderPlacedEvent(Long orderId, List<String> productIds, int totalQuantity) {
        this.orderId = orderId;
        this.productIds = productIds;
        this.totalQuantity = totalQuantity;
    }

    public Long getOrderId() { return orderId; }
    public List<String> getProductIds() { return productIds; }
    public int getTotalQuantity() { return totalQuantity; }
}
