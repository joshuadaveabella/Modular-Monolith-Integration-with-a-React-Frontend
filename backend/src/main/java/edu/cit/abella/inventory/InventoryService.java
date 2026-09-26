package edu.cit.abella.inventory;

import java.util.List;

// The module boundary. Order and the new AutoReorderListener/
// InventoryReplenishmentListener depend on this interface and the two
// public DTOs - nothing else in this package is visible to them.
public interface InventoryService {

    InventoryItem getItem(String productId);

    List<InventoryItem> getAllItems();

    ReservationResult reserve(String productId, int quantity);

    ReservationResult restock(String productId, int quantity);
}
