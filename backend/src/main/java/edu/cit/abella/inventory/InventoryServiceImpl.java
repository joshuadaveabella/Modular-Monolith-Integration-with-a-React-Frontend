package edu.cit.abella.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Package-private (no "public" modifier). Spring can still find and wire
// this up via component scanning + the InventoryService interface type,
// but no class outside this package can reference InventoryServiceImpl
// directly - only through the interface. This is what "Order module may
// depend only on the InventoryService interface" actually enforces at
// compile time, not just by convention.
@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public InventoryItem getItem(String productId) {
        return inventoryRepository.findById(productId)
                .map(this::toItem)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productId));
    }

    @Override
    @Transactional
    public ReservationResult reserve(String productId, int quantity) {
        InventoryEntity entity = inventoryRepository.findById(productId).orElse(null);

        if (entity == null) {
            return new ReservationResult(false, "Product not found", null);
        }

        if (quantity <= 0) {
            return new ReservationResult(false, "Quantity must be greater than zero", toItem(entity));
        }

        if (entity.getStock() < quantity) {
            return new ReservationResult(false, "Insufficient stock", toItem(entity));
        }

        entity.setStock(entity.getStock() - quantity);
        inventoryRepository.save(entity);

        return new ReservationResult(true, null, toItem(entity));
    }

    private InventoryItem toItem(InventoryEntity entity) {
        return new InventoryItem(entity.getProductId(), entity.getName(), entity.getStock());
    }
}
