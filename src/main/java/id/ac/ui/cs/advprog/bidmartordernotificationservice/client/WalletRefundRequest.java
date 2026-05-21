package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

public record WalletRefundRequest(
        String buyerId,
        long amountCents,
        String orderId
) {
}
