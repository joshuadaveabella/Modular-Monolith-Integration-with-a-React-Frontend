package edu.cit.abella.shop;

import edu.cit.abella.inventory.InventoryItem;

public class OrderResponse {

    private final String status;
    private final String reason;
    private final InventoryItem inventory;

    public OrderResponse(String status, String reason, InventoryItem inventory) {
        this.status = status;
        this.reason = reason;
        this.inventory = inventory;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public InventoryItem getInventory() {
        return inventory;
    }
}
