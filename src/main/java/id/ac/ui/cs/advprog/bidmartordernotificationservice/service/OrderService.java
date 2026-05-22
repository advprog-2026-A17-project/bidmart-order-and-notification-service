package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.OpenDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateOrderStatusRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;

import java.util.List;

public interface OrderService {
    BidmartOrder createOrder(CreateOrderRequest request);

    EventOrderCreationResult createOrderFromAuctionWon(AuctionWonEventRequest request);

    BidmartOrder getOrder(String orderId);

    List<BidmartOrder> listOrdersForUser(String userId);

    List<BidmartOrder> listDisputedOrdersForAdmin();

    BidmartOrder updateShipping(String orderId, UpdateOrderStatusRequest request);

    BidmartOrder confirmReceipt(String orderId);

    BidmartOrder openDispute(String orderId, OpenDisputeRequest request);

    BidmartOrder resolveDispute(String orderId, ResolveDisputeRequest request, String resolvedBy);
}
