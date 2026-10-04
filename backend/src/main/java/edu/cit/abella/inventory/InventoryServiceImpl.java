package edu.cit.abella.inventory;

import edu.cit.abella.events.InventoryStockChangedEvent;
import edu.cit.abella.events.LowStockEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        // Task 3 (Lab 4): "Any change in your Inventory... publishes the new
        // available quantity to Tiangge within 30 seconds... Do not publish
        // on a timer." This event is the mechanism - Inventory itself has
        // no idea Tiangge exists; something downstream decides what to do
        // with it.
        eventPublisher.publishEvent(new InventoryStockChangedEvent(entity.getProductId(), entity.getStock()));

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

        eventPublisher.publishEvent(new InventoryStockChangedEvent(entity.getProductId(), entity.getStock()));

        return new ReservationResult(true, null, toItem(entity));
    }

    private InventoryItem toItem(InventoryEntity entity) {
        return new InventoryItem(entity.getProductId(), entity.getName(), entity.getStock());
    }
}
