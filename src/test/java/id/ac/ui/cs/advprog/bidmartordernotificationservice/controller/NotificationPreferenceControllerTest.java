package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationPreferenceService;
import org.junit.jupiter.api.Tag;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class NotificationPreferenceControllerTest {

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @Test
    void getPreferencesDelegatesToService() {
        NotificationPreferenceController controller =
                new NotificationPreferenceController(notificationPreferenceService);
        NotificationPreferenceResponse expected = new NotificationPreferenceResponse(
                "buyer-1", true, true, true, Instant.parse("2026-05-21T10:00:00Z")
        );
        when(notificationPreferenceService.getForUser("buyer-1")).thenReturn(expected);

        NotificationPreferenceResponse response = controller.getPreferences("buyer-1");

        assertEquals(expected, response);
        verify(notificationPreferenceService).getForUser("buyer-1");
    }

    @Test
    void updatePreferencesDelegatesToService() {
        NotificationPreferenceController controller =
                new NotificationPreferenceController(notificationPreferenceService);
        UpdateNotificationPreferenceRequest request =
                new UpdateNotificationPreferenceRequest(false, true, false);
        NotificationPreferenceResponse expected = new NotificationPreferenceResponse(
                "buyer-2", false, true, false, Instant.parse("2026-05-21T10:00:00Z")
        );
        when(notificationPreferenceService.updateForUser("buyer-2", request)).thenReturn(expected);

        NotificationPreferenceResponse response = controller.updatePreferences("buyer-2", request);

        assertEquals(expected, response);
        verify(notificationPreferenceService).updateForUser("buyer-2", request);
    }
}
