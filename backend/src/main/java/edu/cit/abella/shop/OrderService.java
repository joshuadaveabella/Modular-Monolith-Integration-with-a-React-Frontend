package edu.cit.abella.shop;

import edu.cit.abella.events.OrderPlacedEvent;
import edu.cit.abella.events.OrderRejectedEvent;
import edu.cit.abella.inventory.InventoryItem;
import edu.cit.abella.inventory.InventoryService;
import edu.cit.abella.inventory.ReservationResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Note what this class does NOT import: nothing from edu.cit.abella.supplier.
// Order has zero knowledge that LegacySupply, or any supplier, exists.
// Its relationship to restocking is entirely indirect, via events that
// Inventory reacts to.
@Service
public class OrderService {

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(InventoryService inventoryService,
                        OrderRepository orderRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse placeOrder(List<OrderRequest.LineItem> requestedItems) {
        Map<String, Integer> wanted = new LinkedHashMap<>();
        for (OrderRequest.LineItem line : requestedItems) {
            wanted.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }

        Map<String, InventoryItem> stockByProduct = inventoryService.getAllItems().stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        List<ItemOutcome> outcomes = new ArrayList<>();
        String rejectionReason = null;

        for (Map.Entry<String, Integer> entry : wanted.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            InventoryItem stock = stockByProduct.get(productId);

            if (stock == null) {
                outcomes.add(new ItemOutcome(productId, quantity, "PRODUCT_NOT_FOUND"));
                if (rejectionReason == null) rejectionReason = "Unknown product: " + productId;
            } else if (quantity <= 0) {
                outcomes.add(new ItemOutcome(productId, quantity, "INVALID_QUANTITY"));
                if (rejectionReason == null) rejectionReason = "Quantity must be greater than zero for " + productId;
            } else if (stock.getStock() < quantity) {
                outcomes.add(new ItemOutcome(productId, quantity, "INSUFFICIENT_STOCK"));
                if (rejectionReason == null) {
                    rejectionReason = "Insufficient stock for " + productId
                            + " (requested " + quantity + ", available " + stock.getStock() + ")";
                }
            } else {
                outcomes.add(new ItemOutcome(productId, quantity, "NOT_ATTEMPTED"));
            }
        }

        if (rejectionReason != null) {
            OrderEntity order = null;
            Long orderId = null;

            boolean allProductsExist = outcomes.stream()
                    .noneMatch(o -> "PRODUCT_NOT_FOUND".equals(o.getOutcome()));

            if (allProductsExist) {
                order = new OrderEntity();
                order.setStatus("REJECTED");
                order.setReason(rejectionReason);
                wanted.forEach(order::addItem);
                orderRepository.save(order);
                orderId = order.getOrderId();
            }

            eventPublisher.publishEvent(new OrderRejectedEvent(orderId, rejectionReason));

            return new OrderResponse(orderId, "REJECTED", rejectionReason,
                    outcomes, inventoryService.getAllItems());
        }

        List<ItemOutcome> reserved = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : wanted.entrySet()) {
            ReservationResult result = inventoryService.reserve(entry.getKey(), entry.getValue());
            if (!result.isSuccess()) {
                throw new IllegalStateException(
                        "Reservation failed after validation for " + entry.getKey() + ": " + result.getReason());
            }
            reserved.add(new ItemOutcome(entry.getKey(), entry.getValue(), "RESERVED"));
        }

        OrderEntity order = new OrderEntity();
        order.setStatus("CONFIRMED");
        order.setReason(null);
        wanted.forEach(order::addItem);
        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(
                order.getOrderId(),
                new ArrayList<>(wanted.keySet()),
                wanted.values().stream().mapToInt(Integer::intValue).sum()));

        return new OrderResponse(order.getOrderId(), "CONFIRMED", null,
                reserved, inventoryService.getAllItems());
    }

    @Transactional
    public OrderEntity cancelOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new OrderAlreadyCancelledException(orderId);
        }
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new OrderNotCancellableException(orderId, order.getStatus());
        }

        for (OrderItemEntity item : order.getItems()) {
            inventoryService.restock(item.getProductId(), item.getQuantity());
        }

        order.setStatus("CANCELLED");
        order.setReason("Cancelled by user");
        orderRepository.save(order);

        return order;
    }

    public List<OrderSummary> getOrderHistory() {
        return orderRepository.findAllByOrderByOrderIdDesc().stream()
                .map(o -> new OrderSummary(
                        o.getOrderId(), o.getStatus(), o.getReason(), o.getCreatedAt(),
                        o.getItems().stream()
                                .map(i -> new OrderSummary.Line(i.getProductId(), i.getQuantity()))
                                .toList()))
                .toList();
    }

    public List<InventoryItem> currentInventory() {
        return inventoryService.getAllItems();
    }
}
