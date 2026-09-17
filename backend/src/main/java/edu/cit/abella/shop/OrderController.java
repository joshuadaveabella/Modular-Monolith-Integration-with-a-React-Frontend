package edu.cit.abella.shop;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        // A malformed request (no items at all, or a line missing fields) is
        // a genuine 400. Business outcomes - unknown product, not enough
        // stock - come back as 200 with status "REJECTED" and a reason.
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "items must contain at least one line"));
        }
        for (OrderRequest.LineItem line : request.getItems()) {
            if (line.getProductId() == null || line.getProductId().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "each item requires a productId"));
            }
            if (line.getQuantity() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "each item requires a quantity"));
            }
        }

        return ResponseEntity.ok(orderService.placeOrder(request.getItems()));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        OrderEntity cancelled = orderService.cancelOrder(orderId);

        return ResponseEntity.ok(Map.of(
                "orderId", cancelled.getOrderId(),
                "status", cancelled.getStatus(),
                "message", "Order cancelled and stock returned",
                "inventory", orderService.currentInventory()
        ));
    }

    @GetMapping
    public ResponseEntity<?> getOrders() {
        return ResponseEntity.ok(orderService.getOrderHistory());
    }

    // --- error mapping ---

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<?> handleNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({OrderAlreadyCancelledException.class, OrderNotCancellableException.class})
    public ResponseEntity<?> handleConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }
}
