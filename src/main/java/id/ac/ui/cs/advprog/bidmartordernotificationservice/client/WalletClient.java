package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

public interface WalletClient {
    void payoutSeller(String sellerId, long amount, String orderId);

    void creditSellerEscrow(String sellerId, long amount, String auctionId);

    void refundBuyer(String buyerId, long amount);
}
