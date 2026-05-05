package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartNotification;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final String ORDER_CREATED = "ORDER_CREATED";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse notifyOrderCreated(BidmartOrder order) {
        String sourceEventId = sourceEventId(order, ORDER_CREATED);
        BidmartNotification notification = notificationRepository
                .findByUserIdAndTypeAndSourceEventId(order.getBuyerId(), ORDER_CREATED, sourceEventId)
                .orElseGet(() -> notificationRepository.save(BidmartNotification.create(
                        order.getBuyerId(),
                        ORDER_CREATED,
                        "Order created",
                        "Your winning auction has been converted into an order.",
                        sourceEventId
                )));
        NotificationResponse response = NotificationResponse.from(notification);
        messagingTemplate.convertAndSendToUser(order.getBuyerId(), "/queue/notifications", response);
        return response;
    }

    private String sourceEventId(BidmartOrder order, String type) {
        String source = order.getSourceEventId() == null ? order.getId() : order.getSourceEventId();
        return source + ":" + type;
    }
}
