package edu.cit.abella.events;

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

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getRemainingStock() { return remainingStock; }
    public int getThreshold() { return threshold; }
}
