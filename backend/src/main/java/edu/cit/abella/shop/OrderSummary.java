package edu.cit.abella.shop;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSummary {

    private final Long orderId;
    private final String status;
    private final String reason;
    private final LocalDateTime createdAt;
    private final List<Line> items;

    public OrderSummary(Long orderId, String status, String reason,
                        LocalDateTime createdAt, List<Line> items) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Line> getItems() {
        return items;
    }

    public static class Line {

        private final String productId;
        private final int quantity;

        public Line(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
