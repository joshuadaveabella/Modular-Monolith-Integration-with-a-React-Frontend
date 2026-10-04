package edu.cit.abella.inventory;

import java.util.List;

public interface InventoryService {
    InventoryItem getItem(String productId);
    List<InventoryItem> getAllItems();
    ReservationResult reserve(String productId, int quantity);
    ReservationResult restock(String productId, int quantity);
}
