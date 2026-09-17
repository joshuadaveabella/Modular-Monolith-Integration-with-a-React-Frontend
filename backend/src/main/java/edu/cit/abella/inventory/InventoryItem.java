package edu.cit.abella.inventory;

// Public - part of the module's published boundary, along with
// InventoryService and ReservationResult.
public class InventoryItem {

    private final String productId;
    private final String name;
    private final int stock;

    public InventoryItem(String productId, String name, int stock) {
        this.productId = productId;
        this.name = name;
        this.stock = stock;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }
}
