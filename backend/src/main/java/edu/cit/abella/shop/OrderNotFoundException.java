package edu.cit.abella.shop;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("Order " + orderId + " not found");
    }
}
