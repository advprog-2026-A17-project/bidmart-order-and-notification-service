package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.PushSubscription;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LogPushNotificationSenderTest {

    @Mock
    private PushSubscriptionService pushSubscriptionService;

    @InjectMocks
    private LogPushNotificationSender sender;

    @Test
    void sendPushLogsDeviceCountForUser() {
        when(pushSubscriptionService.listForUser("buyer-1")).thenReturn(List.of(
                PushSubscription.create("buyer-1", "https://push.example/1", "p256dh", "auth"),
                PushSubscription.create("buyer-1", "https://push.example/2", "p256dh-2", "auth-2")
        ));

        sender.sendPush("buyer-1", "Auction won", "You won auction auction-1.");

        verify(pushSubscriptionService).listForUser("buyer-1");
    }
}
