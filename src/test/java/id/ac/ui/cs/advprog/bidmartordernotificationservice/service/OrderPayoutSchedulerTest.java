package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.WalletClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
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
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, 5);

        BidmartOrder order = confirmedOrder("seller-ok", new BigDecimal("12500"));
        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(order));

        scheduler.releaseConfirmedOrderPayouts();

        verify(walletClient).payoutSeller(eq("seller-ok"), eq(12500L), eq(order.getId()));
        verify(orderRepository).findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class));
    }

    @Test
    void releaseConfirmedOrderPayoutsShouldContinueWhenWalletPayoutFails() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, 5);

        BidmartOrder failing = confirmedOrder("seller-fail", new BigDecimal("15000"));
        BidmartOrder success = confirmedOrder("seller-ok", new BigDecimal("20000"));
        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(failing, success));

        doThrow(new RuntimeException("wallet down")).when(walletClient)
                .payoutSeller(eq("seller-fail"), eq(15000L), eq(failing.getId()));

        scheduler.releaseConfirmedOrderPayouts();

        verify(walletClient).payoutSeller(eq("seller-fail"), eq(15000L), eq(failing.getId()));
        verify(walletClient).payoutSeller(eq("seller-ok"), eq(20000L), eq(success.getId()));
        // failing order should not throw out and stop loop
        verify(walletClient, never()).payoutSeller(eq("seller-never"), any(Long.class), any(String.class));
    }

    @Test
    void releaseConfirmedOrderPayoutsShouldHandleNullAmountAsZero() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        WalletClient walletClient = mock(WalletClient.class);
        OrderPayoutScheduler scheduler = new OrderPayoutScheduler(orderRepository, walletClient, 5);

        BidmartOrder order = confirmedOrder("seller-null", null);
        when(orderRepository.findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(order));

        scheduler.releaseConfirmedOrderPayouts();

        verify(walletClient).payoutSeller(eq("seller-null"), eq(0L), eq(order.getId()));
    }
}

