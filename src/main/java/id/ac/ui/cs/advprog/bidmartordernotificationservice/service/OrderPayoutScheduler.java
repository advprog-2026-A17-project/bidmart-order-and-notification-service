package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.WalletClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class OrderPayoutScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderPayoutScheduler.class);
    private static final long MAX_PAYOUT_DELAY_MINUTES = 5;

    private final OrderRepository orderRepository;
    private final WalletClient walletClient;
    private final NotificationService notificationService;
    private final Duration payoutDelay;

    public OrderPayoutScheduler(
            OrderRepository orderRepository,
            WalletClient walletClient,
            NotificationService notificationService,
            @Value("${bidmart.order.payout-delay-minutes:5}") long payoutDelayMinutes
    ) {
        this.orderRepository = orderRepository;
        this.walletClient = walletClient;
        this.notificationService = notificationService;
        this.payoutDelay = Duration.ofMinutes(Math.max(0, Math.min(payoutDelayMinutes, MAX_PAYOUT_DELAY_MINUTES)));
    }

    @Scheduled(fixedDelayString = "${bidmart.order.payout-scheduler-interval-ms:60000}")
    @Transactional
    public void releaseConfirmedOrderPayouts() {
        Instant cutoff = Instant.now().minus(payoutDelay);
        List<BidmartOrder> eligibleOrders = orderRepository
                .findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(OrderStatus.CONFIRMED, cutoff);

        for (BidmartOrder order : eligibleOrders) {
            try {
                long amount = toRupiah(order.getFinalPrice());
                walletClient.payoutSeller(order.getSellerId(), amount, order.getId());
                order.markPayoutReleased();
                notifySellerPayoutReleased(order, amount);
            } catch (RuntimeException ex) {
                logger.warn("Failed to release payout for order {}: {}", order.getId(), ex.getMessage());
            }
        }
    }

    private void notifySellerPayoutReleased(BidmartOrder order, long amount) {
        try {
            notificationService.notifySellerPayoutReleased(order, amount);
        } catch (RuntimeException notificationError) {
            logger.warn("Failed to notify seller payout for order {}: {}", order.getId(), notificationError.getMessage());
        }
    }

    private long toRupiah(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.setScale(0, RoundingMode.CEILING).longValueExact();
    }
}
