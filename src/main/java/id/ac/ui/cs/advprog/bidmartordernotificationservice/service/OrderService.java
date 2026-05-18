package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateOrderStatusRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.OrderNotFoundException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
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
                    notificationService.notifyOrderCreated(order);
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
}
