package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateNotificationReadStatusRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationService;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> listForUser(@RequestHeader("X-User-Id") String userId) {
        return notificationService.listForUser(userId);
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse getForUser(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String notificationId
    ) {
        return notificationService.getForUser(userId, notificationId);
    }

    @PatchMapping("/{notificationId}/read-status")
    public NotificationResponse updateReadStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String notificationId,
            @RequestBody UpdateNotificationReadStatusRequest request
    ) {
        return notificationService.updateReadStatus(userId, notificationId, request.read());
    }
}
