package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

public record UpdateNotificationPreferenceRequest(
        Boolean email,
        Boolean push,
        Boolean inApp
) {
}
