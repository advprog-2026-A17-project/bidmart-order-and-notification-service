package id.ac.ui.cs.advprog.bidmartordernotificationservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthEventConsumer {

    private static final String USER_DISABLED = "UserDisabled";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    public AuthEventConsumer(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${bidmart.rabbitmq.order.auth-events-queue:order-notification.auth-events}")
    public void consume(Message message) throws JsonProcessingException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Object eventTypeHeader = message.getMessageProperties().getHeaders().get("eventType");
        String eventType = eventTypeHeader == null ? "" : eventTypeHeader.toString();
        if (!USER_DISABLED.equals(eventType)) {
            return;
        }

        JsonNode payload = objectMapper.readTree(body);
        String userId = payload.path("userId").asText("");
        String email = payload.path("email").asText("");
        if (userId.isBlank()) {
            return;
        }

        String dedupeKey = userId + ":" + payload.path("occurredAt").asText("");
        if (!dedupeKey.isBlank() && !processedKeys.add(dedupeKey)) {
            return;
        }
        notificationService.notifyUserDisabled(userId, email, dedupeKey);
    }
}
