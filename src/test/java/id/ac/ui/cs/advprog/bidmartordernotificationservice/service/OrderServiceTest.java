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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WalletClient walletClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderReturnsExistingOrderForSameAuction() {
        BidmartOrder existing = BidmartOrder.create(
                "auction-1",
                "listing-1",
                "seller-1",
                "buyer-1",
                new BigDecimal("10000"),
                "Address",
                null
        );
        when(orderRepository.findByAuctionId("auction-1")).thenReturn(Optional.of(existing));

        BidmartOrder result = orderService.createOrder(new CreateOrderRequest(
                "auction-1",
                "listing-1",
                "seller-1",
                "buyer-1",
                new BigDecimal("20000"),
                "Other address"
        ));

        assertEquals(existing.getId(), result.getId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderFromAuctionWonIsIdempotentBySourceEventId() {
        BidmartOrder existing = BidmartOrder.create(
                "auction-2",
                "listing-2",
                "seller-2",
                "buyer-2",
                new BigDecimal("15000"),
                "Jakarta",
                "evt-2"
        );
        when(orderRepository.findBySourceEventId("evt-2")).thenReturn(Optional.of(existing));

        EventOrderCreationResult result = orderService.createOrderFromAuctionWon(new AuctionWonEventRequest(
                "evt-2",
                "auction-2",
                "listing-2",
                "seller-2",
                "buyer-2",
                new BigDecimal("15000"),
                "Jakarta"
        ));

        assertFalse(result.created());
        assertEquals(existing.getId(), result.order().getId());
        verify(notificationService, never()).notifyOrderCreated(any());
    }

    @Test
    void createOrderFromAuctionWonFallsBackToAuctionIdLookup() {
        BidmartOrder existing = BidmartOrder.create(
                "auction-3",
                "listing-3",
                "seller-3",
                "buyer-3",
                new BigDecimal("12000"),
                "Bandung",
                null
        );
        when(orderRepository.findBySourceEventId("evt-3")).thenReturn(Optional.empty());
        when(orderRepository.findByAuctionId("auction-3")).thenReturn(Optional.of(existing));

        EventOrderCreationResult result = orderService.createOrderFromAuctionWon(new AuctionWonEventRequest(
                "evt-3",
                "auction-3",
                "listing-3",
                "seller-3",
                "buyer-3",
                new BigDecimal("12000"),
                "Bandung"
        ));

        assertFalse(result.created());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderFromAuctionWonCreatesOrderAndNotifiesBuyer() {
        when(orderRepository.findBySourceEventId("evt-4")).thenReturn(Optional.empty());
        when(orderRepository.findByAuctionId("auction-4")).thenReturn(Optional.empty());
        when(orderRepository.save(any(BidmartOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventOrderCreationResult result = orderService.createOrderFromAuctionWon(new AuctionWonEventRequest(
                "evt-4",
                "auction-4",
                "listing-4",
                "seller-4",
                "buyer-4",
                new BigDecimal("9900"),
                "Surabaya"
        ));

        assertTrue(result.created());
        assertEquals("auction-4", result.order().getAuctionId());
        verify(notificationService).notifyOrderCreated(result.order());
    }

    @Test
    void updateShippingGeneratesTrackingNumberWhenShippedWithoutTracking() {
        BidmartOrder order = BidmartOrder.create(
                "auction-5",
                "listing-5",
                "seller-5",
                "buyer-5",
                new BigDecimal("5000"),
                "Address",
                null
        );
        order.updateShipping(OrderStatus.PACKED, "TRK-OLD", "JNE");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        BidmartOrder updated = orderService.updateShipping(
                order.getId(),
                new UpdateOrderStatusRequest(OrderStatus.SHIPPED, null, "JNE")
        );

        assertEquals(OrderStatus.SHIPPED, updated.getStatus());
        assertNotNull(updated.getTrackingNumber());
        assertTrue(updated.getTrackingNumber().startsWith("TRK-"));
    }

    @Test
    void getOrderThrowsWhenMissing() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder("missing"));
    }

    @Test
    void listOrdersForUserDelegatesToRepository() {
        BidmartOrder order = BidmartOrder.create(
                "auction-6",
                "listing-6",
                "seller-6",
                "buyer-6",
                new BigDecimal("3000"),
                "Address",
                null
        );
        when(orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc("buyer-6", "buyer-6"))
                .thenReturn(List.of(order));

        List<BidmartOrder> orders = orderService.listOrdersForUser("buyer-6");

        assertEquals(1, orders.size());
    }

    @Test
    void openDisputeShouldMarkOrderDisputedAndNotifySeller() {
        BidmartOrder order = BidmartOrder.create(
                "auction-dispute",
                "listing-dispute",
                "seller-1",
                "buyer-1",
                new BigDecimal("150"),
                "Address",
                null
        );
        order.updateShipping(OrderStatus.PACKED, "TRK", "JNE");
        order.updateShipping(OrderStatus.SHIPPED, "TRK", "JNE");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(BidmartOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BidmartOrder disputed = orderService.openDispute(
                order.getId(),
                new OpenDisputeRequest("Never received", "No delivery")
        );

        assertEquals(OrderStatus.DISPUTED, disputed.getStatus());
        assertEquals("Never received", disputed.getDisputeReason());
        verify(notificationService).notifyOrderDisputed(disputed);
    }

    @Test
    void resolveDisputeShouldRefundBuyerWhenBuyerWins() {
        BidmartOrder order = BidmartOrder.create(
                "auction-resolve",
                "listing-resolve",
                "seller-2",
                "buyer-2",
                new BigDecimal("200"),
                "Address",
                null
        );
        order.updateShipping(OrderStatus.PACKED, "TRK", "JNE");
        order.updateShipping(OrderStatus.SHIPPED, "TRK", "JNE");
        order.openDispute("Damaged item", "Box empty");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(BidmartOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BidmartOrder resolved = orderService.resolveDispute(
                order.getId(),
                new ResolveDisputeRequest(DisputeWinner.BUYER),
                "admin-1"
        );

        assertEquals(OrderStatus.REFUNDED, resolved.getStatus());
        verify(walletClient).refundBuyer("buyer-2", 20000L);
        verify(notificationService).notifyDisputeResolved(resolved);
    }
}
