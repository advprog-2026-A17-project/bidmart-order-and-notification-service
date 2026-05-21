package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.PushSubscription;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.lang.reflect.Field;
import java.security.Security;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WebPushNotificationSenderTest {

    private static final String PUBLIC_KEY =
            "BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U";
    private static final String PRIVATE_KEY = "UUxI4O8-FbRouAevSmBQ6o18hgE4eSG5ZwY74UuBuUk";
    private static final String SUBSCRIPTION_P256DH =
            "BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U";
    private static final String SUBSCRIPTION_AUTH = "tbkVIqhO92Q6ljw96RU+Mw==";
    @Mock
    private PushSubscriptionService pushSubscriptionService;

    private ObjectMapper objectMapper;
    private WebPushNotificationSender sender;

    @BeforeEach
    void setUp() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        objectMapper = new ObjectMapper();
        sender = new WebPushNotificationSender(
                pushSubscriptionService,
                objectMapper,
                PUBLIC_KEY,
                PRIVATE_KEY,
                "mailto:test@bidmart.local"
        );
    }

    @Test
    void sendPushReturnsEarlyWhenUserHasNoSubscriptions() {
        when(pushSubscriptionService.listForUser("buyer-1")).thenReturn(List.of());

        sender.sendPush("buyer-1", "Title", "Message");

        verify(pushSubscriptionService).listForUser("buyer-1");
    }

    @Test
    void sendPushSkipsDeliveryWhenPayloadCannotBeEncoded() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("json error"));
        WebPushNotificationSender failingSender = new WebPushNotificationSender(
                pushSubscriptionService,
                failingMapper,
                PUBLIC_KEY,
                PRIVATE_KEY,
                "mailto:test@bidmart.local"
        );
        PushSubscription stored = PushSubscription.create(
                "buyer-5",
                "https://push.example/subscription-4",
                "p256dh-key",
                "auth-key"
        );
        when(pushSubscriptionService.listForUser("buyer-5")).thenReturn(List.of(stored));

        PushService pushService = mock(PushService.class);
        replacePushService(pushService, failingSender);

        failingSender.sendPush("buyer-5", "Title", "Message");

        verify(pushService, never()).send(any(Notification.class));
    }

    private void replacePushService(PushService pushService) throws Exception {
        replacePushService(pushService, sender);
    }

    private static void replacePushService(PushService pushService, WebPushNotificationSender target)
            throws Exception {
        Field field = WebPushNotificationSender.class.getDeclaredField("pushService");
        field.setAccessible(true);
        field.set(target, pushService);
    }
}
