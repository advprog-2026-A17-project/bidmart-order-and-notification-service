package id.ac.ui.cs.advprog.bidmartordernotificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.metrics.BidmartOrderMetrics;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private BidmartOrderMetrics orderMetrics;

    private AuthEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuthEventConsumer(new ObjectMapper(), notificationService, orderMetrics);
    }

    @Test
    void userDisabledEventSendsNotificationAndRecordsMetrics() throws Exception {
        Message message = messageWithEventType(
                "UserDisabled",
                """
                {"userId":"user-1","email":"buyer@test.com","occurredAt":"2026-05-21T10:00:00Z"}
                """
        );

        consumer.consume(message);

        verify(orderMetrics).recordRabbitConsumed();
        verify(notificationService).notifyUserDisabled(
                eq("user-1"),
                eq("buyer@test.com"),
                eq("user-1:2026-05-21T10:00:00Z")
        );
        verify(orderMetrics).recordNotificationSent();
    }

    @Test
    void missingEventTypeHeaderIsIgnored() throws Exception {
        MessageProperties properties = new MessageProperties();
        Message message = new Message(
                """
                {"userId":"user-2","email":"buyer@test.com","occurredAt":"2026-05-21T10:00:00Z"}
                """.getBytes(StandardCharsets.UTF_8),
                properties
        );

        consumer.consume(message);

        verify(orderMetrics).recordRabbitConsumed();
        verify(notificationService, never()).notifyUserDisabled(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void nonUserDisabledEventTypeIsIgnored() throws Exception {
        Message message = messageWithEventType(
                "UserRegistered",
                """
                {"userId":"user-1","email":"buyer@test.com"}
                """
        );

        consumer.consume(message);

        verify(orderMetrics).recordRabbitConsumed();
        verify(notificationService, never()).notifyUserDisabled(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(orderMetrics, never()).recordNotificationSent();
    }

    @Test
    void blankUserIdSkipsNotification() throws Exception {
        Message message = messageWithEventType(
                "UserDisabled",
                """
                {"userId":"","email":"buyer@test.com","occurredAt":"2026-05-21T10:00:00Z"}
                """
        );

        consumer.consume(message);

        verify(orderMetrics).recordRabbitConsumed();
        verify(notificationService, never()).notifyUserDisabled(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(orderMetrics, never()).recordNotificationSent();
    }

    @Test
    void duplicateOccurredAtIsIgnored() throws Exception {
        String body = """
                {"userId":"user-dup","email":"dup@test.com","occurredAt":"2026-05-21T11:00:00Z"}
                """;
        Message first = messageWithEventType("UserDisabled", body);
        Message second = messageWithEventType("UserDisabled", body);

        consumer.consume(first);
        consumer.consume(second);

        verify(notificationService, times(1)).notifyUserDisabled(
                eq("user-dup"),
                eq("dup@test.com"),
                eq("user-dup:2026-05-21T11:00:00Z")
        );
        verify(orderMetrics, times(1)).recordNotificationSent();
        verify(orderMetrics, times(2)).recordRabbitConsumed();
    }

    private static Message messageWithEventType(String eventType, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("eventType", eventType);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
