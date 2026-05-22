package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

public record WalletRefundRequest(
        String buyerId,
        long amount,
        String orderId
) {
}
