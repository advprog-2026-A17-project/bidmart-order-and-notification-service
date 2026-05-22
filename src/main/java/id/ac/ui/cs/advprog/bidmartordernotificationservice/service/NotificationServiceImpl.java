package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.NotificationNotFoundException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartNotification;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.NotificationRepository;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification.NotificationChannel;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification.NotificationChannels.EmailNotificationChannel;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification.NotificationChannels.InAppNotificationChannel;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification.NotificationChannels.PushNotificationChannel;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification.NotificationEnvelope;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification.NotificationTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final List<NotificationChannel> channels;
    private final TaskExecutor notificationTaskExecutor;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate,
            NotificationPreferenceService notificationPreferenceService,
            ExternalNotificationDispatcher externalNotificationDispatcher,
            @Qualifier("notificationTaskExecutor") TaskExecutor notificationTaskExecutor
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceService = notificationPreferenceService;
        this.notificationTaskExecutor = notificationTaskExecutor == null
                ? Runnable::run
                : notificationTaskExecutor;
        this.channels = new ArrayList<>();
        this.channels.add(new InAppNotificationChannel(messagingTemplate));
        this.channels.add(new EmailNotificationChannel(externalNotificationDispatcher));
        this.channels.add(new PushNotificationChannel(externalNotificationDispatcher));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getForUser(String userId, String notificationId) {
        BidmartNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationResponse updateReadStatus(String userId, String notificationId, boolean read) {
        BidmartNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (read) {
            notification.markAsRead();
        } else {
            notification.markAsUnread();
        }
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse notifyOrderCreated(BidmartOrder order) {
        return createAndPublish(order.getBuyerId(), NotificationTemplateFactory.orderCreated(order));
    }

    @Transactional
    public NotificationResponse notifyBidPlaced(
            String userId,
            String auctionId,
            long amountCents,
            String eventId
    ) {
        return createAndPublish(userId, NotificationTemplateFactory.bidPlaced(auctionId, amountCents, eventId));
    }

    @Transactional
    public NotificationResponse notifyOutbid(
            String userId,
            String auctionId,
            long amountCents,
            String eventId
    ) {
        return createAndPublish(userId, NotificationTemplateFactory.outbid(auctionId, amountCents, eventId));
    }

    @Transactional
    public NotificationResponse notifyAuctionWon(String userId, String auctionId, String eventId) {
        return createAndPublish(userId, NotificationTemplateFactory.auctionWon(auctionId, eventId));
    }

    @Transactional
    public NotificationResponse notifyOrderDisputed(BidmartOrder order) {
        return createAndPublish(order.getSellerId(), NotificationTemplateFactory.orderDisputed(order));
    }

    @Transactional
    public NotificationResponse notifyDisputeResolved(BidmartOrder order) {
        createAndPublish(order.getBuyerId(), NotificationTemplateFactory.disputeResolvedForBuyer(order));
        return createAndPublish(order.getSellerId(), NotificationTemplateFactory.disputeResolvedForSeller(order));
    }

    @Transactional
    public NotificationResponse notifySellerPayoutReleased(BidmartOrder order, long amount) {
        return createAndPublish(order.getSellerId(), NotificationTemplateFactory.sellerPayoutReleased(order));
    }

    @Transactional
    public NotificationResponse notifyUserDisabled(String userId, String email, String dedupeKey) {
        return createAndPublish(userId, NotificationTemplateFactory.userDisabled(userId, email, dedupeKey));
    }

    @Transactional
    public NotificationResponse notifyAuctionEnded(
            String userId,
            String auctionId,
            boolean sold,
            String eventId
    ) {
        return createAndPublish(userId, NotificationTemplateFactory.auctionEnded(auctionId, sold, eventId));
    }

    private NotificationResponse createAndPublish(String userId, NotificationEnvelope envelope) {
        BidmartNotification notification = notificationRepository
                .findByUserIdAndTypeAndSourceEventId(userId, envelope.type(), envelope.sourceEventId())
                .orElseGet(() -> notificationRepository.save(BidmartNotification.create(
                        userId,
                        envelope.type(),
                        envelope.title(),
                        envelope.message(),
                        envelope.sourceEventId()
                )));
        NotificationResponse response = NotificationResponse.from(notification);
        var preferences = notificationPreferenceService.findOrCreate(userId);
        for (NotificationChannel channel : channels) {
            notificationTaskExecutor.execute(() -> publishSafely(channel, userId, envelope, response, preferences));
        }
        return response;
    }

    private void publishSafely(
            NotificationChannel channel,
            String userId,
            NotificationEnvelope envelope,
            NotificationResponse response,
            id.ac.ui.cs.advprog.bidmartordernotificationservice.model.NotificationPreference preferences
    ) {
        try {
            channel.publish(userId, envelope, response, preferences);
        } catch (Exception e) {
            log.warn(
                    "Notification channel {} failed for user {} type {}: {}",
                    channel.getClass().getSimpleName(),
                    userId,
                    envelope.type(),
                    e.getMessage()
            );
        }
    }

    static String formatIdrFromCents(long amountCents) {
        return NotificationTemplateFactory.formatIdrFromCents(amountCents);
    }
}
