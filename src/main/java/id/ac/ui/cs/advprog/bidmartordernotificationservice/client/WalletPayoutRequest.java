package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

public record WalletPayoutRequest(
        String sellerId,
        long amountCents,
        String orderId
) {
}
