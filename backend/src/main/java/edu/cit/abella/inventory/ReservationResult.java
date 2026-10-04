package edu.cit.abella.inventory;

public class ReservationResult {

    private final boolean success;
    private final String reason;
    private final InventoryItem inventory;

    public ReservationResult(boolean success, String reason, InventoryItem inventory) {
        this.success = success;
        this.reason = reason;
        this.inventory = inventory;
    }

    public boolean isSuccess() { return success; }
    public String getReason() { return reason; }
    public InventoryItem getInventory() { return inventory; }
}
