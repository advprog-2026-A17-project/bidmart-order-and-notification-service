package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.WalletClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.DisputeWinner;
import org.springframework.web.client.RestClientException;

public interface DisputeResolutionStrategy {

    void apply(BidmartOrder order, WalletClient walletClient);

    static DisputeResolutionStrategy forWinner(DisputeWinner winner) {
        if (winner == DisputeWinner.BUYER) {
            return new BuyerWinsDisputeStrategy();
        }
        return new SellerWinsDisputeStrategy();
    }
}

final class BuyerWinsDisputeStrategy implements DisputeResolutionStrategy {
    @Override
    public void apply(BidmartOrder order, WalletClient walletClient) {
        try {
            walletClient.refundBuyer(order.getBuyerId(), OrderMoney.toRupiah(order.getFinalPrice()));
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to refund buyer wallet balance", ex);
        }
    }
}

final class SellerWinsDisputeStrategy implements DisputeResolutionStrategy {
    @Override
    public void apply(BidmartOrder order, WalletClient walletClient) {
        walletClient.creditSellerEscrow(
                order.getSellerId(),
                OrderMoney.toSellerEscrowRupiah(order.getFinalPrice()),
                order.getAuctionId()
        );
    }
}
