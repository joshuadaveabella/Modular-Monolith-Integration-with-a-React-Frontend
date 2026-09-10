package edu.cit.abella.shop;

import edu.cit.abella.inventory.InventoryService;
import edu.cit.abella.inventory.ReservationResult;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    // Constructor injection of the INTERFACE only. Spring resolves this to
    // the single InventoryServiceImpl bean at runtime, but this class never
    // references that implementation type - it couldn't even if it wanted
    // to, since InventoryServiceImpl is package-private to a different package.
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;

    public OrderService(InventoryService inventoryService, OrderRepository orderRepository) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
    }

    public OrderResponse placeOrder(String productId, int quantity) {
        // In-process call - a plain Java method call on an interface, not a
        // network request. No HTTP, no serialization, no separate deployment.
        ReservationResult reservation = inventoryService.reserve(productId, quantity);

        if (reservation.getInventory() == null) {
            // Product doesn't exist at all. orders.product_id has a foreign
            // key into inventory, so there's nothing valid to persist here -
            // we just report the rejection without writing a row.
            return new OrderResponse("REJECTED", reservation.getReason(), null);
        }

        OrderEntity order = new OrderEntity();
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus(reservation.isSuccess() ? "CONFIRMED" : "REJECTED");
        order.setReason(reservation.isSuccess() ? null : reservation.getReason());

        orderRepository.save(order);

        return new OrderResponse(order.getStatus(), order.getReason(), reservation.getInventory());
    }
}
