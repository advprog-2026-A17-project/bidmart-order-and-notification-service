package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.WalletClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.OpenDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateOrderStatusRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.OrderNotFoundException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.OrderRepository;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order.OrderCommandHandlers.ConfirmReceiptHandler;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order.OrderCommandHandlers.CreateOrderFromAuctionWonHandler;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order.OrderCommandHandlers.CreateOrderHandler;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order.OrderCommandHandlers.OpenDisputeHandler;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order.OrderCommandHandlers.ResolveDisputeHandler;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order.OrderCommandHandlers.UpdateShippingHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CreateOrderHandler createOrderHandler;
    private final CreateOrderFromAuctionWonHandler createOrderFromAuctionWonHandler;
    private final UpdateShippingHandler updateShippingHandler;
    private final ConfirmReceiptHandler confirmReceiptHandler;
    private final OpenDisputeHandler openDisputeHandler;
    private final ResolveDisputeHandler resolveDisputeHandler;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            NotificationService notificationService,
            WalletClient walletClient
    ) {
        this.orderRepository = orderRepository;
        this.createOrderHandler = new CreateOrderHandler(orderRepository);
        this.createOrderFromAuctionWonHandler = new CreateOrderFromAuctionWonHandler(orderRepository, notificationService);
        this.updateShippingHandler = new UpdateShippingHandler(orderRepository);
        this.confirmReceiptHandler = new ConfirmReceiptHandler(orderRepository, walletClient);
        this.openDisputeHandler = new OpenDisputeHandler(orderRepository, notificationService);
        this.resolveDisputeHandler = new ResolveDisputeHandler(orderRepository, walletClient, notificationService);
    }

    @Transactional
    public BidmartOrder createOrder(CreateOrderRequest request) {
        return createOrderHandler.handle(request);
    }

    @Transactional
    public EventOrderCreationResult createOrderFromAuctionWon(AuctionWonEventRequest request) {
        return createOrderFromAuctionWonHandler.handle(request);
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

    @Transactional(readOnly = true)
    public List<BidmartOrder> listDisputedOrdersForAdmin() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.DISPUTED);
    }

    @Transactional
    public BidmartOrder updateShipping(String orderId, UpdateOrderStatusRequest request) {
        return updateShippingHandler.handle(orderId, request);
    }

    @Transactional
    public BidmartOrder confirmReceipt(String orderId) {
        return confirmReceiptHandler.handle(orderId);
    }

    @Transactional
    public BidmartOrder openDispute(String orderId, OpenDisputeRequest request) {
        return openDisputeHandler.handle(orderId, request);
    }

    @Transactional
    public BidmartOrder resolveDispute(String orderId, ResolveDisputeRequest request, String resolvedBy) {
        return resolveDisputeHandler.handle(orderId, request, resolvedBy);
    }
}
