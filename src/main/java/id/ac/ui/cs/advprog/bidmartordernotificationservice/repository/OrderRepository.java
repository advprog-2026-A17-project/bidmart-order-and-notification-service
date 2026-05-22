package id.ac.ui.cs.advprog.bidmartordernotificationservice.repository;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<BidmartOrder, String> {

    Optional<BidmartOrder> findByAuctionId(String auctionId);

    Optional<BidmartOrder> findBySourceEventId(String sourceEventId);

    List<BidmartOrder> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(String buyerId, String sellerId);

    List<BidmartOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<BidmartOrder> findByStatusAndPayoutReleasedAtIsNullAndConfirmedAtBefore(
            OrderStatus status,
            Instant confirmedBefore
    );
}
