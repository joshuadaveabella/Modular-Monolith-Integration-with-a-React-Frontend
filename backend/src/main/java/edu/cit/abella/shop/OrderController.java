package edu.cit.abella.shop;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        // Malformed request (missing fields) -> genuine 400.
        // Everything else (unknown product, insufficient stock, bad quantity
        // value) is a valid business outcome and comes back as 200 with
        // status: "REJECTED" and a reason - not an HTTP error.
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "productId is required"));
        }
        if (request.getQuantity() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "quantity is required"));
        }

        OrderResponse response = orderService.placeOrder(request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(response);
    }
}
