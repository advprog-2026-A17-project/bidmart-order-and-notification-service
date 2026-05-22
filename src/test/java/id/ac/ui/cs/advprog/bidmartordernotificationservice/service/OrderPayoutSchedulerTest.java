package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.WalletClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPayoutSchedulerTest {

    private static BidmartOrder confirmedOrder(String sellerId, BigDecimal amount) {
        BidmartOrder order = BidmartOrder.create(
                "auction-1",
                "listing-1",
                sellerId,
                "buyer-1",
                amount,
                "Jl. Test 123",
                "evt-1"
        );
        order.updateShipping(OrderStatus.PACKED, "TRK-1", "JNE");
        order.updateShipping(OrderStatus.SHIPPED, "TRK-1", "JNE");
        order.confirmReceipt();
        return order;
    }

    @Test
    void releaseConfirmedOrderPayoutsShouldPayoutAndMarkReleased() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        NotificationService notificationService = mock(NotificationService.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, notificationService, 5);

        BidmartOrder order = confirmedOrder("seller-ok", new BigDecimal("125.00"));
        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(order));

        scheduler.releaseConfirmedOrderPayouts();

        verify(walletClient).payoutSeller(eq("seller-ok"), eq(125L), eq(order.getId()));
        verify(notificationService).notifySellerPayoutReleased(eq(order), eq(125L));
        verify(orderRepository).findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class));
    }

    @Test
    void releaseConfirmedOrderPayoutsShouldContinueWhenWalletPayoutFails() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        NotificationService notificationService = mock(NotificationService.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, notificationService, 5);

        BidmartOrder failing = confirmedOrder("seller-fail", new BigDecimal("150.00"));
        BidmartOrder success = confirmedOrder("seller-ok", new BigDecimal("200.00"));
        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(failing, success));

        doThrow(new org.springframework.web.client.RestClientException("wallet down")).when(walletClient)
                .payoutSeller(eq("seller-fail"), eq(150L), eq(failing.getId()));

        scheduler.releaseConfirmedOrderPayouts();

        verify(walletClient).payoutSeller(eq("seller-fail"), eq(150L), eq(failing.getId()));
        verify(walletClient).payoutSeller(eq("seller-ok"), eq(200L), eq(success.getId()));
        // failing order should not throw out and stop loop
        verify(walletClient, never()).payoutSeller(eq("seller-never"), any(Long.class), any(String.class));
    }

    @Test
    void releaseConfirmedOrderPayoutsShouldHandleNullAmountAsZero() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        NotificationService notificationService = mock(NotificationService.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, notificationService, 5);

        BidmartOrder order = confirmedOrder("seller-null", null);
        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(order));

        scheduler.releaseConfirmedOrderPayouts();

        verify(walletClient).payoutSeller(eq("seller-null"), eq(0L), eq(order.getId()));
    }

    @Test
    void releaseConfirmedOrderPayoutsMarksOrderReleasedOnSuccess() throws Exception {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        NotificationService notificationService = mock(NotificationService.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, notificationService, 5);

        BidmartOrder order = confirmedOrder("seller-done", new BigDecimal("99.00"));
        Field confirmedAt = BidmartOrder.class.getDeclaredField("confirmedAt");
        confirmedAt.setAccessible(true);
        confirmedAt.set(order, Instant.now().minus(10, ChronoUnit.MINUTES));

        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(order));

        scheduler.releaseConfirmedOrderPayouts();

        org.junit.jupiter.api.Assertions.assertNotNull(order.getPayoutReleasedAt());
        verify(walletClient).payoutSeller(eq("seller-done"), eq(99L), eq(order.getId()));
    }

    @Test
    void releaseConfirmedOrderPayoutsRoundsFractionalRupiahUp() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        NotificationService notificationService = mock(NotificationService.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, notificationService, 5);

        BidmartOrder order = confirmedOrder("seller-fractional", new BigDecimal("99.50"));
        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(order));

        scheduler.releaseConfirmedOrderPayouts();

        verify(walletClient).payoutSeller(eq("seller-fractional"), eq(100L), eq(order.getId()));
        verify(notificationService).notifySellerPayoutReleased(eq(order), eq(100L));
    }

    @Test
    void releaseConfirmedOrderPayoutsCapsConfiguredDelayAtFiveMinutes() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        NotificationService notificationService = mock(NotificationService.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, notificationService, 60);

        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of());

        scheduler.releaseConfirmedOrderPayouts();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(orderRepository).findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), cutoff.capture());
        org.junit.jupiter.api.Assertions.assertTrue(cutoff.getValue().isAfter(Instant.now().minus(6, ChronoUnit.MINUTES)));
        org.junit.jupiter.api.Assertions.assertTrue(cutoff.getValue().isBefore(Instant.now().minus(4, ChronoUnit.MINUTES)));
    }
}
