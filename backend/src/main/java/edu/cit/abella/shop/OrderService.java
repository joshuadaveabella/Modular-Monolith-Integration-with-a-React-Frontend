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

@Service
public class OrderService {

    // Still constructor-injected by INTERFACE only. OrderService cannot
    // name InventoryServiceImpl even if it wanted to.
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    // OrderService publishes events; it has no reference to the Notification
    // module and does not import anything from that package.
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
        // Collapse duplicate product IDs first. Without this, a cart holding
        // P200 x6 twice would validate as 6 <= 10 twice and then reserve 12
        // from a stock of 10 - the validate-then-reserve split would be
        // defeated by the cart itself.
        Map<String, Integer> wanted = new LinkedHashMap<>();
        for (OrderRequest.LineItem line : requestedItems) {
            wanted.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }

        // ---- PHASE 1: validate everything, reserve nothing ----
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
                if (rejectionReason == null) {
                    rejectionReason = "Unknown product: " + productId;
                }
            } else if (quantity <= 0) {
                outcomes.add(new ItemOutcome(productId, quantity, "INVALID_QUANTITY"));
                if (rejectionReason == null) {
                    rejectionReason = "Quantity must be greater than zero for " + productId;
                }
            } else if (stock.getStock() < quantity) {
                outcomes.add(new ItemOutcome(productId, quantity, "INSUFFICIENT_STOCK"));
                if (rejectionReason == null) {
                    rejectionReason = "Insufficient stock for " + productId
                            + " (requested " + quantity + ", available " + stock.getStock() + ")";
                }
            } else {
                // Passes validation, but nothing has been reserved yet.
                outcomes.add(new ItemOutcome(productId, quantity, "NOT_ATTEMPTED"));
            }
        }

        // ---- REJECTED PATH: not a single reserve() call was made ----
        if (rejectionReason != null) {
            OrderEntity order = null;
            Long orderId = null;

            // Only persist the order row if every product actually exists,
            // since order_items.product_id has a foreign key into inventory.
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

        // ---- PHASE 2: everything validated, now reserve ----
        List<ItemOutcome> reserved = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : wanted.entrySet()) {
            ReservationResult result = inventoryService.reserve(entry.getKey(), entry.getValue());

            if (!result.isSuccess()) {
                // Should be unreachable given phase 1, but if it happens
                // (e.g. a concurrent order drained stock between the two
                // phases), throwing rolls back the whole @Transactional
                // method - including any reserve() calls already made in
                // this loop, since they share this transaction.
                throw new IllegalStateException(
                        "Reservation failed after validation for " + entry.getKey()
                                + ": " + result.getReason());
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

        // A REJECTED order never reserved anything, so restocking it would
        // invent stock out of nothing. Treated as a conflict for the same
        // reason an already-cancelled order is.
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new OrderNotCancellableException(orderId, order.getStatus());
        }

        // Reverse direction of the same in-process call used to place the
        // order: Order -> InventoryService, this time returning stock.
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
                        o.getOrderId(),
                        o.getStatus(),
                        o.getReason(),
                        o.getCreatedAt(),
                        o.getItems().stream()
                                .map(i -> new OrderSummary.Line(i.getProductId(), i.getQuantity()))
                                .toList()))
                .toList();
    }

    public List<InventoryItem> currentInventory() {
        return inventoryService.getAllItems();
    }
}
