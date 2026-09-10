package edu.cit.abella.inventory;

// This interface, plus InventoryItem and ReservationResult, IS the module
// boundary. The Order module depends on this and nothing else in this
// package - it never sees InventoryEntity, InventoryRepository, or the
// concrete InventoryServiceImpl.
public interface InventoryService {

    InventoryItem getItem(String productId);

    ReservationResult reserve(String productId, int quantity);
}
