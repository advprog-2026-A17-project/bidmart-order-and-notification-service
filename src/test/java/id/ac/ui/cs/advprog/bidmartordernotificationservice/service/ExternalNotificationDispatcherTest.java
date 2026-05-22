package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.AuthClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalNotificationDispatcherTest {

    @Mock
    private AuthClient authClient;

    @Mock
    private NotificationEmailSender notificationEmailSender;

    @Mock
    private PushNotificationSender pushNotificationSender;

    @InjectMocks
    private ExternalNotificationDispatcher dispatcher;

    @Test
    void sendEmailDispatchesWhenUserEmailExists() {
        when(authClient.fetchUserEmail("buyer-1")).thenReturn(Optional.of("buyer@example.com"));

        dispatcher.sendEmail("buyer-1", "Outbid", "You were outbid on auction auction-1.");

        verify(notificationEmailSender).sendNotificationEmail(
                "buyer@example.com",
                "Outbid",
                "You were outbid on auction auction-1."
        );
    }

    @Test
    void sendPushDelegatesToPushSender() {
        dispatcher.sendPush("buyer-3", "Auction won", "You won auction auction-3.");

        verify(pushNotificationSender).sendPush("buyer-3", "Auction won", "You won auction auction-3.");
    }

    @Test
    void sendEmailSkipsWhenUserEmailMissing() {
        when(authClient.fetchUserEmail("buyer-2")).thenReturn(Optional.empty());

        dispatcher.sendEmail("buyer-2", "Bid placed", "Message");

        verify(notificationEmailSender, never()).sendNotificationEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
