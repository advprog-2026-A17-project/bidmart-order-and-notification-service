package id.ac.ui.cs.advprog.bidmartordernotificationservice.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
