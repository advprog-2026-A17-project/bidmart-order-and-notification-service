package id.ac.ui.cs.advprog.bidmartordernotificationservice.controller;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.RegisterPushSubscriptionRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.VapidPublicKeyResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.PushSubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/push")
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;
    private final String vapidPublicKey;

    public PushSubscriptionController(
            PushSubscriptionService pushSubscriptionService,
            @Value("${bidmart.notification.push.vapid.public-key:}") String vapidPublicKey
    ) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.vapidPublicKey = vapidPublicKey == null ? "" : vapidPublicKey.trim();
    }

    @GetMapping("/vapid-public-key")
    public VapidPublicKeyResponse vapidPublicKey() {
        return new VapidPublicKeyResponse(vapidPublicKey, !vapidPublicKey.isBlank());
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody RegisterPushSubscriptionRequest request
    ) {
        pushSubscriptionService.register(userId, request);
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@RequestHeader("X-User-Id") String userId) {
        pushSubscriptionService.removeAllForUser(userId);
    }
}
