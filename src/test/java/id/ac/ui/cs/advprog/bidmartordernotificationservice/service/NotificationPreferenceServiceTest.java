package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository repository;

    @InjectMocks
    private NotificationPreferenceService service;

    @Test
    void getForUserCreatesDefaultsWhenMissing() {
        when(repository.findById("buyer-1")).thenReturn(Optional.empty());
        when(repository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceResponse response = service.getForUser("buyer-1");

        assertEquals("buyer-1", response.userId());
        assertTrue(response.email());
        assertTrue(response.push());
        assertTrue(response.inApp());
    }

    @Test
    void updateForUserPersistsPartialChanges() {
        NotificationPreference existing = NotificationPreference.defaults("buyer-2");
        when(repository.findById("buyer-2")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        NotificationPreferenceResponse response = service.updateForUser(
                "buyer-2",
                new UpdateNotificationPreferenceRequest(false, true, null)
        );

        assertFalse(response.email());
        assertTrue(response.push());
        assertTrue(response.inApp());
        verify(repository).save(existing);
    }
}
