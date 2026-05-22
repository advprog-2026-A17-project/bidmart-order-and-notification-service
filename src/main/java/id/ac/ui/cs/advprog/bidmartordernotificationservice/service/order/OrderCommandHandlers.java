package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order;

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
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.EventOrderCreationResult;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationService;

import java.util.UUID;

public final class OrderCommandHandlers {

    private OrderCommandHandlers() {
    }

    public static final class CreateOrderHandler {
        private final OrderRepository orderRepository;

        public CreateOrderHandler(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        public BidmartOrder handle(CreateOrderRequest request) {
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
    }

    public static final class CreateOrderFromAuctionWonHandler {
        private final OrderRepository orderRepository;
        private final NotificationService notificationService;

        public CreateOrderFromAuctionWonHandler(OrderRepository orderRepository, NotificationService notificationService) {
            this.orderRepository = orderRepository;
            this.notificationService = notificationService;
        }

        public EventOrderCreationResult handle(AuctionWonEventRequest request) {
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
    }

    public static final class UpdateShippingHandler {
        private final OrderRepository orderRepository;

        public UpdateShippingHandler(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        public BidmartOrder handle(String orderId, UpdateOrderStatusRequest request) {
            BidmartOrder order = getOrder(orderId);
            String trackingNumber = request.trackingNumber();
            if (request.status() == OrderStatus.SHIPPED && (trackingNumber == null || trackingNumber.isBlank())) {
                trackingNumber = generateTrackingNumber();
            }
            OrderStates.forStatus(order.getStatus()).updateShipping(order, request.status(), trackingNumber, request.carrier());
            return order;
        }

        private BidmartOrder getOrder(String orderId) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
        }

        private String generateTrackingNumber() {
            return "TRK-" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    public static final class ConfirmReceiptHandler {
        private final OrderRepository orderRepository;
        private final WalletClient walletClient;

        public ConfirmReceiptHandler(OrderRepository orderRepository, WalletClient walletClient) {
            this.orderRepository = orderRepository;
            this.walletClient = walletClient;
        }

        public BidmartOrder handle(String orderId) {
            BidmartOrder order = getOrder(orderId);
            OrderStates.forStatus(order.getStatus()).confirmReceipt(order);
            walletClient.creditSellerEscrow(
                    order.getSellerId(),
                    OrderMoney.toSellerEscrowRupiah(order.getFinalPrice()),
                    order.getAuctionId()
            );
            return order;
        }

        private BidmartOrder getOrder(String orderId) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
        }
    }

    public static final class OpenDisputeHandler {
        private final OrderRepository orderRepository;
        private final NotificationService notificationService;

        public OpenDisputeHandler(OrderRepository orderRepository, NotificationService notificationService) {
            this.orderRepository = orderRepository;
            this.notificationService = notificationService;
        }

        public BidmartOrder handle(String orderId, OpenDisputeRequest request) {
            BidmartOrder order = getOrder(orderId);
            OrderStates.forStatus(order.getStatus()).openDispute(order, request.reason(), request.details());
            BidmartOrder saved = orderRepository.save(order);
            notificationService.notifyOrderDisputed(saved);
            return saved;
        }

        private BidmartOrder getOrder(String orderId) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
        }
    }

    public static final class ResolveDisputeHandler {
        private final OrderRepository orderRepository;
        private final WalletClient walletClient;
        private final NotificationService notificationService;

        public ResolveDisputeHandler(OrderRepository orderRepository, WalletClient walletClient, NotificationService notificationService) {
            this.orderRepository = orderRepository;
            this.walletClient = walletClient;
            this.notificationService = notificationService;
        }

        public BidmartOrder handle(String orderId, ResolveDisputeRequest request, String resolvedBy) {
            BidmartOrder order = getOrder(orderId);
            OrderStates.forStatus(order.getStatus()).resolveDispute(order, request.winner(), resolvedBy);
            DisputeResolutionStrategy.forWinner(request.winner()).apply(order, walletClient);
            BidmartOrder saved = orderRepository.save(order);
            notificationService.notifyDisputeResolved(saved);
            return saved;
        }

        private BidmartOrder getOrder(String orderId) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
        }
    }
}
