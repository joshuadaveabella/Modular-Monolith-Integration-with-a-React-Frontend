package edu.cit.abella.shop;

import edu.cit.abella.events.BackorderResolvedEvent;
import edu.cit.abella.events.OrderPlacedEvent;
import edu.cit.abella.events.OrderRejectedEvent;
import edu.cit.abella.inventory.InventoryItem;
import edu.cit.abella.inventory.InventoryService;
import edu.cit.abella.inventory.ReservationResult;
import edu.cit.abella.supplier.SupplierGateway;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Note what this class still does NOT import: anything from
// edu.cit.abella.channel. Task 4 says Tiangge orders "go through the same
// Order and Inventory logic" as React orders - this class is that shared
// logic. The channel module calls placeOrder(items, true) below; it never
// gets a Tiangge-specific code path of its own.
@Service
public class OrderService {
    private int alreadyBackorderedUnits(String productId) {
        return orderRepository.findAllByStatus("BACKORDERED").stream()
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getProductId().equals(productId))
                .mapToInt(OrderItemEntity::getQuantity)
                .sum();
    }
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    // Added in Lab 4, used only to answer "is a restock already on the way"
    // for the BACKORDERED decision - never anything LegacySupply-specific.
    private final SupplierGateway supplierGateway;

    public OrderService(InventoryService inventoryService,
                        OrderRepository orderRepository,
                        ApplicationEventPublisher eventPublisher,
                        SupplierGateway supplierGateway) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.supplierGateway = supplierGateway;
    }

    /** Unchanged entry point for the React UI - never backorders. */
    @Transactional
    public OrderResponse placeOrder(List<OrderRequest.LineItem> requestedItems) {
        return place(requestedItems, false);
    }

    /**
     * Entry point used by the channel module for Tiangge orders. The ONLY
     * difference from the method above is that a stock shortfall may
     * resolve to BACKORDERED instead of REJECTED, when every short line
     * already has an open LegacySupply purchase order.
     */
    @Transactional
    public OrderResponse placeOrder(List<OrderRequest.LineItem> requestedItems, boolean allowBackorder) {
        return place(requestedItems, allowBackorder);
    }

    private OrderResponse place(List<OrderRequest.LineItem> requestedItems, boolean allowBackorder) {
        Map<String, Integer> wanted = new LinkedHashMap<>();
        for (OrderRequest.LineItem line : requestedItems) {
            wanted.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }

        Map<String, InventoryItem> stockByProduct = inventoryService.getAllItems().stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        List<ItemOutcome> outcomes = new ArrayList<>();
        String rejectionReason = null;
        boolean everyShortfallHasIncomingStock = true;

        for (Map.Entry<String, Integer> entry : wanted.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            InventoryItem stock = stockByProduct.get(productId);

            if (stock == null) {
                outcomes.add(new ItemOutcome(productId, quantity, "PRODUCT_NOT_FOUND"));
                if (rejectionReason == null) rejectionReason = "Unknown product: " + productId;
                everyShortfallHasIncomingStock = false; // an unknown product can never be backordered
            } else if (quantity <= 0) {
                outcomes.add(new ItemOutcome(productId, quantity, "INVALID_QUANTITY"));
                if (rejectionReason == null) rejectionReason = "Quantity must be greater than zero for " + productId;
                everyShortfallHasIncomingStock = false;
            } else if (stock.getStock() < quantity) {
                outcomes.add(new ItemOutcome(productId, quantity, "INSUFFICIENT_STOCK"));
                if (rejectionReason == null) {
                    rejectionReason = "Insufficient stock for " + productId
                            + " (requested " + quantity + ", available " + stock.getStock() + ")";
                }
                int openUnits = supplierGateway.getOpenOrderUnits(productId);
                int alreadyPromised = alreadyBackorderedUnits(productId);
                if (openUnits - alreadyPromised < quantity) {
                    everyShortfallHasIncomingStock = false;
                }
            } else {
                outcomes.add(new ItemOutcome(productId, quantity, "NOT_ATTEMPTED"));
            }
        }

        if (rejectionReason != null) {
            if (allowBackorder && everyShortfallHasIncomingStock) {
                return backorder(wanted, outcomes);
            }
            return reject(wanted, outcomes, rejectionReason);
        }

        return confirm(wanted);
    }

    private OrderResponse reject(Map<String, Integer> wanted, List<ItemOutcome> outcomes, String rejectionReason) {
        OrderEntity order = null;
        Long orderId = null;

        boolean allProductsExist = outcomes.stream().noneMatch(o -> "PRODUCT_NOT_FOUND".equals(o.getOutcome()));

        if (allProductsExist) {
            order = new OrderEntity();
            order.setStatus("REJECTED");
            order.setReason(rejectionReason);
            wanted.forEach(order::addItem);
            orderRepository.save(order);
            orderId = order.getOrderId();
        }

        eventPublisher.publishEvent(new OrderRejectedEvent(orderId, rejectionReason));

        return new OrderResponse(orderId, "REJECTED", rejectionReason, outcomes, inventoryService.getAllItems());
    }

    private OrderResponse backorder(Map<String, Integer> wanted, List<ItemOutcome> outcomes) {
        // Nothing is reserved yet - that happens when the backorder resolves.
        OrderEntity order = new OrderEntity();
        order.setStatus("BACKORDERED");
        order.setReason("Awaiting supplier delivery");
        wanted.forEach(order::addItem);
        orderRepository.save(order);

        List<ItemOutcome> backorderedOutcomes = outcomes.stream()
                .map(o -> new ItemOutcome(o.getProductId(), o.getQuantity(),
                        "INSUFFICIENT_STOCK".equals(o.getOutcome()) ? "BACKORDERED" : o.getOutcome()))
                .toList();

        return new OrderResponse(order.getOrderId(), "BACKORDERED", order.getReason(),
                backorderedOutcomes, inventoryService.getAllItems());
    }

    private OrderResponse confirm(Map<String, Integer> wanted) {
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
        wanted.forEach(order::addItem);
        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(
                order.getOrderId(), new ArrayList<>(wanted.keySet()),
                wanted.values().stream().mapToInt(Integer::intValue).sum()));

        return new OrderResponse(order.getOrderId(), "CONFIRMED", null, reserved, inventoryService.getAllItems());
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

    /**
     * Task 6: "When the delivery arrives, your app reserves the stock and
     * resolves the backorder to ACCEPTED, or to CANCELLED if it still
     * can't be filled." Called by a listener reacting to
     * SupplierOrderDeliveredEvent - this method itself has no idea that
     * event came from LegacySupply, or that the order came from Tiangge.
     * It publishes BackorderResolvedEvent so whatever DOES need to tell
     * Tiangge can react, without this class knowing Tiangge exists.
     */
    @Transactional
    public void resolveBackordersForProduct(String productId) {
        List<OrderEntity> backordered = orderRepository.findAllByStatus("BACKORDERED").stream()
                .filter(o -> o.getItems().stream().anyMatch(i -> i.getProductId().equals(productId)))
                .toList();

        for (OrderEntity order : backordered) {
            resolveOne(order);
        }
    }

    private void resolveOne(OrderEntity order) {
        // All-or-nothing, same rule as every other reservation in this app.
        List<ReservationResult> results = new ArrayList<>();
        boolean allOk = true;

        for (OrderItemEntity item : order.getItems()) {
            InventoryItem current = inventoryService.getItem(item.getProductId());
            if (current.getStock() < item.getQuantity()) {
                allOk = false;
                break;
            }
        }

        if (allOk) {
            for (OrderItemEntity item : order.getItems()) {
                ReservationResult result = inventoryService.reserve(item.getProductId(), item.getQuantity());
                results.add(result);
                if (!result.isSuccess()) {
                    allOk = false; // lost a race with something else between the check above and now
                }
            }
        }

        if (allOk) {
            order.setStatus("CONFIRMED");
            order.setReason(null);
            orderRepository.save(order);

            eventPublisher.publishEvent(new OrderPlacedEvent(
                    order.getOrderId(),
                    order.getItems().stream().map(OrderItemEntity::getProductId).toList(),
                    order.getItems().stream().mapToInt(OrderItemEntity::getQuantity).sum()));
            eventPublisher.publishEvent(new BackorderResolvedEvent(order.getOrderId(), "CONFIRMED"));
        } else {
            // Partial reservations (if any) made in the failed attempt above
            // are rolled back automatically - this whole method is
            // @Transactional, so nothing partially reserved here persists.
            order.setStatus("CANCELLED");
            order.setReason("Could not be fulfilled after supplier delivery");
            orderRepository.save(order);

            eventPublisher.publishEvent(new BackorderResolvedEvent(order.getOrderId(), "CANCELLED"));
        }
    }

    /** Used by channel's FeedPoller to re-derive a decision on retry. */
    public OrderSummary getOrderSummary(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return new OrderSummary(
                order.getOrderId(), order.getStatus(), order.getReason(), order.getCreatedAt(),
                order.getItems().stream()
                        .map(i -> new OrderSummary.Line(i.getProductId(), i.getQuantity()))
                        .toList());
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

