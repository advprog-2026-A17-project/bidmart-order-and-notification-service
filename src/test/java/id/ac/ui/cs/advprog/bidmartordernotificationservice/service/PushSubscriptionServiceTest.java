package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.RegisterPushSubscriptionRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.PushSubscription;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock
    private PushSubscriptionRepository repository;

    @InjectMocks
    private PushSubscriptionService service;

    @Test
    void registerPersistsNewSubscription() {
        when(repository.findByUserIdAndEndpoint("buyer-1", "https://push.example/1"))
                .thenReturn(Optional.empty());
        when(repository.save(any(PushSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(
                "buyer-1",
                new RegisterPushSubscriptionRequest(
                        "https://push.example/1",
                        new RegisterPushSubscriptionRequest.PushSubscriptionKeys("p256dh-key", "auth-key")
                )
        );

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(repository).save(captor.capture());
        assertEquals("buyer-1", captor.getValue().getUserId());
        assertEquals("https://push.example/1", captor.getValue().getEndpoint());
    }

    @Test
    void registerUpdatesExistingSubscriptionKeys() {
        PushSubscription existing = PushSubscription.create(
                "buyer-2",
                "https://push.example/2",
                "old-p256dh",
                "old-auth"
        );
        when(repository.findByUserIdAndEndpoint("buyer-2", "https://push.example/2"))
                .thenReturn(java.util.Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.register(
                "buyer-2",
                new RegisterPushSubscriptionRequest(
                        "https://push.example/2",
                        new RegisterPushSubscriptionRequest.PushSubscriptionKeys("new-p256dh", "new-auth")
                )
        );

        assertEquals("new-p256dh", existing.getP256dhKey());
        assertEquals("new-auth", existing.getAuthKey());
        verify(repository).save(existing);
    }

    @Test
    void registerRejectsMissingKeys() {
        assertThrows(IllegalArgumentException.class, () -> service.register(
                "buyer-3",
                new RegisterPushSubscriptionRequest(
                        "https://push.example/3",
                        null
                )
        ));
    }

    @Test
    void listForUserReturnsRepositoryResults() {
        PushSubscription subscription = PushSubscription.create(
                "buyer-4",
                "https://push.example/4",
                "p256dh",
                "auth"
        );
        when(repository.findByUserId("buyer-4")).thenReturn(java.util.List.of(subscription));

        assertEquals(1, service.listForUser("buyer-4").size());
    }

    @Test
    void removeAllForUserDeletesSubscriptions() {
        service.removeAllForUser("buyer-5");

        verify(repository).deleteByUserId("buyer-5");
    }

    @Test
    void registerRejectsMissingEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> service.register(
                "buyer-1",
                new RegisterPushSubscriptionRequest(
                        " ",
                        new RegisterPushSubscriptionRequest.PushSubscriptionKeys("p256dh-key", "auth-key")
                )
        ));
    }
}
