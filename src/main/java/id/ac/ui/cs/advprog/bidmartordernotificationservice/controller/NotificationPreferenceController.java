package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationPreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;

    public NotificationPreferenceController(NotificationPreferenceService notificationPreferenceService) {
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @GetMapping
    public NotificationPreferenceResponse getPreferences(@RequestHeader("X-User-Id") String userId) {
        return notificationPreferenceService.getForUser(userId);
    }

    @PutMapping
    public NotificationPreferenceResponse updatePreferences(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        return notificationPreferenceService.updateForUser(userId, request);
    }
}
