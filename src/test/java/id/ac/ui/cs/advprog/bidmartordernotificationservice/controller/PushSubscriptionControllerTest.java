package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.RegisterPushSubscriptionRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.VapidPublicKeyResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.PushSubscriptionService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PushSubscriptionControllerTest {

    @Mock
    private PushSubscriptionService pushSubscriptionService;

    @Test
    void vapidPublicKeyExposesConfiguredKey() {
        PushSubscriptionController controller = new PushSubscriptionController(
                pushSubscriptionService,
                " configured-public-key "
        );

        VapidPublicKeyResponse response = controller.vapidPublicKey();

        assertEquals("configured-public-key", response.publicKey());
        assertTrue(response.enabled());
    }

    @Test
    void vapidPublicKeyReportsDisabledWhenBlank() {
        PushSubscriptionController controller = new PushSubscriptionController(pushSubscriptionService, "  ");

        VapidPublicKeyResponse response = controller.vapidPublicKey();

        assertEquals("", response.publicKey());
        assertFalse(response.enabled());
    }

    @Test
    void registerDelegatesToService() {
        PushSubscriptionController controller = new PushSubscriptionController(pushSubscriptionService, "");
        RegisterPushSubscriptionRequest request = new RegisterPushSubscriptionRequest(
                "https://push.example/endpoint",
                new RegisterPushSubscriptionRequest.PushSubscriptionKeys("p256dh", "auth")
        );

        controller.register("buyer-1", request);

        verify(pushSubscriptionService).register("buyer-1", request);
    }

    @Test
    void unregisterRemovesAllSubscriptionsForUser() {
        PushSubscriptionController controller = new PushSubscriptionController(pushSubscriptionService, "");

        controller.unregister("buyer-2");

        verify(pushSubscriptionService).removeAllForUser("buyer-2");
    }
}
