package edu.cit.abella.inventory;

// Public - returned by InventoryService.reserve() to the Order module.
public class ReservationResult {

    private final boolean success;
    private final String reason; // null when success is true
    private final InventoryItem inventory; // null only if the product doesn't exist

    public ReservationResult(boolean success, String reason, InventoryItem inventory) {
        this.success = success;
        this.reason = reason;
        this.inventory = inventory;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    public InventoryItem getInventory() {
        return inventory;
    }
}
