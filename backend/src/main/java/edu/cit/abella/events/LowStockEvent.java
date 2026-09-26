package edu.cit.abella.events;

// Published by Inventory after a successful reserve() leaves a product
// below the configured threshold. In Lab 3 this is consumed by TWO
// listeners: NotificationListener (logs it) and the new AutoReorderListener
// in the inventory module (places a real purchase order via SupplierGateway).
public class LowStockEvent {

    private final String productId;
    private final String productName;
    private final int remainingStock;
    private final int threshold;

    public LowStockEvent(String productId, String productName, int remainingStock, int threshold) {
        this.productId = productId;
        this.productName = productName;
        this.remainingStock = remainingStock;
        this.threshold = threshold;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getRemainingStock() {
        return remainingStock;
    }

    public int getThreshold() {
        return threshold;
    }
}
