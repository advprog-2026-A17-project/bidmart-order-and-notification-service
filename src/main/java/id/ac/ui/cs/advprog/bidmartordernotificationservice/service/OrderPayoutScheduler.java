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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class OrderPayoutScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderPayoutScheduler.class);

    private final OrderRepository orderRepository;
    private final WalletClient walletClient;
    private final Duration payoutDelay;

    public OrderPayoutScheduler(
            OrderRepository orderRepository,
            WalletClient walletClient,
            @Value("${bidmart.order.payout-delay-minutes:5}") long payoutDelayMinutes
    ) {
        this.orderRepository = orderRepository;
        this.walletClient = walletClient;
        this.payoutDelay = Duration.ofMinutes(payoutDelayMinutes);
    }

    @Scheduled(fixedDelayString = "${bidmart.order.payout-scheduler-interval-ms:60000}")
    @Transactional
    public void releaseConfirmedOrderPayouts() {
        Instant cutoff = Instant.now().minus(payoutDelay);
        List<BidmartOrder> eligibleOrders = orderRepository
                .findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(OrderStatus.CONFIRMED, cutoff);

        for (BidmartOrder order : eligibleOrders) {
            long amountCents = toCents(order.getFinalPrice());
            try {
                walletClient.payoutSeller(order.getSellerId(), amountCents, order.getId());
                order.markPayoutReleased();
            } catch (RuntimeException ex) {
                logger.warn("Failed to release payout for order {}: {}", order.getId(), ex.getMessage());
            }
        }
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.UNNECESSARY).longValueExact();
    }
}
