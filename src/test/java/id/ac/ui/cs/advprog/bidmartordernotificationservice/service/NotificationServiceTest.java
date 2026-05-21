package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.NotificationNotFoundException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartNotification;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyBidPlacedPersistsAndPublishesRealtimeMessage() {
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("buyer-1", "BID_PLACED", "evt-1:BID_PLACED"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyBidPlaced(
                "buyer-1",
                "auction-1",
                12_500L,
                "evt-1"
        );

        assertEquals("BID_PLACED", response.type());
        assertEquals("Bid placed", response.title());
        assertEquals("Your bid of IDR 125.00 was placed on auction auction-1.", response.message());
        verify(messagingTemplate).convertAndSendToUser(eq("buyer-1"), eq("/queue/notifications"), any(NotificationResponse.class));
    }

    @Test
    void notifyOutbidFormatsAmountInDollars() {
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("buyer-2", "OUTBID", "evt-2:OUTBID"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyOutbid(
                "buyer-2",
                "auction-2",
                14_000L,
                "evt-2"
        );

        assertEquals("OUTBID", response.type());
        assertEquals("A higher bid of IDR 140.00 was placed on auction auction-2.", response.message());
    }

    @Test
    void notifyAuctionWonAndEndedUseDistinctSourceEventIds() {
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyAuctionWon("buyer-3", "auction-3", "evt-3");
        notificationService.notifyAuctionEnded("seller-3", "auction-3", true, "evt-3");
        notificationService.notifyAuctionEnded("seller-3", "auction-4", false, "evt-4");

        ArgumentCaptor<BidmartNotification> saved = ArgumentCaptor.forClass(BidmartNotification.class);
        verify(notificationRepository, org.mockito.Mockito.times(3)).save(saved.capture());
        assertEquals("evt-3:AUCTION_WON", saved.getAllValues().get(0).getSourceEventId());
        assertEquals("evt-3:AUCTION_ENDED", saved.getAllValues().get(1).getSourceEventId());
        assertEquals("evt-4:AUCTION_ENDED", saved.getAllValues().get(2).getSourceEventId());
    }

    @Test
    void notifyOrderCreatedUsesOrderSourceEventIdWhenPresent() {
        BidmartOrder order = BidmartOrder.create(
                "auction-5",
                "listing-5",
                "seller-5",
                "buyer-5",
                new BigDecimal("9900"),
                "Bandung",
                "evt-order-5"
        );
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("buyer-5", "ORDER_CREATED", "evt-order-5:ORDER_CREATED"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyOrderCreated(order);

        assertEquals("ORDER_CREATED", response.type());
        verify(notificationRepository, never()).findByUserIdAndTypeAndSourceEventId("buyer-5", "ORDER_CREATED", order.getId() + ":ORDER_CREATED");
    }

    @Test
    void createAndPublishIsIdempotentForSameSourceEvent() {
        BidmartNotification existing = BidmartNotification.create(
                "buyer-6",
                "BID_PLACED",
                "Bid placed",
                "Existing",
                "evt-6:BID_PLACED"
        );
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("buyer-6", "BID_PLACED", "evt-6:BID_PLACED"))
                .thenReturn(Optional.of(existing));

        NotificationResponse response = notificationService.notifyBidPlaced(
                "buyer-6",
                "auction-6",
                100L,
                "evt-6"
        );

        assertEquals(existing.getId(), response.id());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getForUserThrowsWhenNotificationMissing() {
        when(notificationRepository.findByIdAndUserId("missing", "buyer-7"))
                .thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getForUser("buyer-7", "missing"));
    }

    @Test
    void updateReadStatusThrowsWhenNotificationMissing() {
        when(notificationRepository.findByIdAndUserId("missing", "buyer-8"))
                .thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.updateReadStatus("buyer-8", "missing", true));
    }

    @Test
    void updateReadStatusCanMarkUnread() {
        BidmartNotification notification = BidmartNotification.create(
                "buyer-8",
                "ORDER_CREATED",
                "Order created",
                "Message",
                "evt-8:ORDER_CREATED"
        );
        notification.markAsRead();
        when(notificationRepository.findByIdAndUserId(notification.getId(), "buyer-8"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.updateReadStatus("buyer-8", notification.getId(), false);

        assertEquals(false, response.read());
        assertEquals("UNREAD", response.status());
    }

    @Test
    void notifyUserDisabledUsesEmailInMessageWhenPresent() {
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("user-1", "USER_DISABLED", "dedupe-1"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyUserDisabled(
                "user-1",
                "buyer@example.com",
                "dedupe-1"
        );

        assertEquals("USER_DISABLED", response.type());
        assertEquals("Your account (buyer@example.com) has been disabled by an administrator.", response.message());
    }

    @Test
    void notifyUserDisabledUsesGenericMessageWhenEmailMissing() {
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("user-2", "USER_DISABLED", "user-2:USER_DISABLED"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyUserDisabled("user-2", null, " ");

        assertEquals("Your account has been disabled by an administrator.", response.message());
    }

    @Test
    void notifyUserDisabledIncludesEmailWhenPresent() {
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("user-1", "USER_DISABLED", "dedupe-1"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyUserDisabled(
                "user-1",
                "user@test.com",
                "dedupe-1"
        );

        assertEquals("USER_DISABLED", response.type());
        assertEquals("Your account (user@test.com) has been disabled by an administrator.", response.message());
    }

    @Test
    void notifyUserDisabledUsesGenericMessageWithoutEmail() {
        when(notificationRepository.findByUserIdAndTypeAndSourceEventId("user-2", "USER_DISABLED", "user-2:USER_DISABLED"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(BidmartNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyUserDisabled("user-2", " ", "");

        assertEquals("Your account has been disabled by an administrator.", response.message());
    }

    @Test
    void listForUserMapsRepositoryResults() {
        BidmartNotification notification = BidmartNotification.create(
                "buyer-9",
                "AUCTION_WON",
                "Auction won",
                "You won",
                "evt-9:AUCTION_WON"
        );
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("buyer-9"))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.listForUser("buyer-9");

        assertEquals(1, responses.size());
        assertEquals("AUCTION_WON", responses.get(0).type());
    }
}
