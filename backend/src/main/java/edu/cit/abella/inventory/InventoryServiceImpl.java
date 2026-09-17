package edu.cit.abella.inventory;

import edu.cit.abella.events.LowStockEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Package-private (no "public" modifier). Spring wires it by interface
// type; no class outside this package can name it. That is what makes
// "Order may depend only on the InventoryService interface" a compiler
// guarantee rather than a convention.
@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    InventoryServiceImpl(InventoryRepository inventoryRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public InventoryItem getItem(String productId) {
        return inventoryRepository.findById(productId)
                .map(this::toItem)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productId));
    }

    @Override
    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll().stream()
                .map(this::toItem)
                .sorted((a, b) -> a.getProductId().compareTo(b.getProductId()))
                .toList();
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

        InventoryItem item = toItem(entity);

        // Low-stock business rule. Published as its own event type so the
        // Notification module can log it separately from order outcomes.
        // Note this fires per successful reserve(), so a multi-item order
        // can emit several LowStock events in one request - one per product
        // that crossed the threshold.
        if (entity.getStock() < lowStockThreshold) {
            eventPublisher.publishEvent(new LowStockEvent(
                    entity.getProductId(), entity.getName(), entity.getStock(), lowStockThreshold));
        }

        return new ReservationResult(true, null, item);
    }

    @Override
    @Transactional
    public ReservationResult restock(String productId, int quantity) {
        InventoryEntity entity = inventoryRepository.findById(productId).orElse(null);

        if (entity == null) {
            return new ReservationResult(false, "Product not found", null);
        }
        if (quantity <= 0) {
            return new ReservationResult(false, "Quantity must be greater than zero", toItem(entity));
        }

        entity.setStock(entity.getStock() + quantity);
        inventoryRepository.save(entity);

        // Deliberately no LowStock event here - restocking moves stock up,
        // so it can only ever resolve a low-stock condition, never cause one.
        return new ReservationResult(true, null, toItem(entity));
    }

    private InventoryItem toItem(InventoryEntity entity) {
        return new InventoryItem(entity.getProductId(), entity.getName(), entity.getStock());
    }
}
