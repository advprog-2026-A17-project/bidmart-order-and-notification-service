package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

public record WalletSellerEscrowRequest(
        String sellerId,
        long amount,
        String correlationId
) {
}
