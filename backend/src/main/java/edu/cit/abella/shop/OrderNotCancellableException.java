package edu.cit.abella.shop;

public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(Long orderId, String status) {
        super("Order " + orderId + " cannot be cancelled because its status is " + status);
    }
}
