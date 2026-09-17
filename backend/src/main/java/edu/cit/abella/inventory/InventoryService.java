package edu.cit.abella.inventory;

import java.util.List;

// The module boundary. Order depends on this interface and the two public
// DTOs above - nothing else in this package is visible to it.
public interface InventoryService {

    InventoryItem getItem(String productId);

    // Used by OrderService to validate an entire multi-item cart in one
    // read before reserving anything, and by GET /api/inventory.
    List<InventoryItem> getAllItems();

    ReservationResult reserve(String productId, int quantity);

    // New in Lab 2: returns quantity to stock when an order is cancelled.
    // The implementation stays package-private, same as reserve().
    ReservationResult restock(String productId, int quantity);
}
