package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.WalletClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.OpenDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateOrderStatusRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.OrderNotFoundException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.DisputeWinner;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final WalletClient walletClient;

    public OrderService(
            OrderRepository orderRepository,
            NotificationService notificationService,
            WalletClient walletClient
    ) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.walletClient = walletClient;
    }

    @Transactional
    public BidmartOrder createOrder(CreateOrderRequest request) {
        return orderRepository.findByAuctionId(request.auctionId())
                .orElseGet(() -> orderRepository.save(BidmartOrder.create(
                        request.auctionId(),
                        request.listingId(),
                        request.sellerId(),
                        request.buyerId(),
                        request.finalPrice(),
                        request.shippingAddress(),
                        null
                )));
    }

    @Transactional
    public EventOrderCreationResult createOrderFromAuctionWon(AuctionWonEventRequest request) {
        return orderRepository.findBySourceEventId(request.eventId())
                .or(() -> orderRepository.findByAuctionId(request.auctionId()))
                .map(order -> new EventOrderCreationResult(order, false))
                .orElseGet(() -> {
                    BidmartOrder order = orderRepository.save(BidmartOrder.create(
                            request.auctionId(),
                            request.listingId(),
                            request.sellerId(),
                            request.buyerId(),
                            request.finalPrice(),
                            request.shippingAddress(),
                            request.eventId()
                    ));
                    try {
                        notificationService.notifyOrderCreated(order);
                    } catch (RuntimeException notificationError) {
                        // Order persistence must succeed even if optional channels fail.
                    }
                    return new EventOrderCreationResult(order, true);
                });
    }

    @Transactional(readOnly = true)
    public BidmartOrder getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<BidmartOrder> listOrdersForUser(String userId) {
        return orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId);
    }

    @Transactional
    public BidmartOrder updateShipping(String orderId, UpdateOrderStatusRequest request) {
        BidmartOrder order = getOrder(orderId);
        String trackingNumber = request.trackingNumber();
        if (request.status() == id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus.SHIPPED
                && (trackingNumber == null || trackingNumber.isBlank())) {
            trackingNumber = generateTrackingNumber();
        }
        order.updateShipping(request.status(), trackingNumber, request.carrier());
        return order;
    }

    private String generateTrackingNumber() {
        return "TRK-" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public BidmartOrder confirmReceipt(String orderId) {
        BidmartOrder order = getOrder(orderId);
        order.confirmReceipt();
        return order;
    }

    @Transactional
    public BidmartOrder openDispute(String orderId, OpenDisputeRequest request) {
        BidmartOrder order = getOrder(orderId);
        order.openDispute(request.reason(), request.details());
        BidmartOrder saved = orderRepository.save(order);
        notificationService.notifyOrderDisputed(saved);
        return saved;
    }

    @Transactional
    public BidmartOrder resolveDispute(String orderId, ResolveDisputeRequest request, String resolvedBy) {
        BidmartOrder order = getOrder(orderId);
        order.resolveDispute(request.winner(), resolvedBy);
        if (request.winner() == DisputeWinner.BUYER) {
            try {
                walletClient.refundBuyer(order.getBuyerId(), toRupiah(order.getFinalPrice()));
            } catch (RestClientException ex) {
                throw new IllegalStateException("Failed to refund buyer wallet balance", ex);
            }
        }
        BidmartOrder saved = orderRepository.save(order);
        notificationService.notifyDisputeResolved(saved);
        return saved;
    }

    private long toRupiah(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.setScale(0, java.math.RoundingMode.UNNECESSARY).longValueExact();
    }
}
