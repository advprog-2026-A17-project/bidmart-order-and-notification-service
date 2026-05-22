package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.NotificationPreference;

import java.time.Instant;

public record NotificationPreferenceResponse(
        String userId,
        boolean email,
        boolean push,
        boolean inApp,
        Instant updatedAt
) {
    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.getUserId(),
                preference.isEmailEnabled(),
                preference.isPushEnabled(),
                preference.isInAppEnabled(),
                preference.getUpdatedAt()
        );
    }
}
