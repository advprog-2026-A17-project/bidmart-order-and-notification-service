package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.RegisterPushSubscriptionRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.PushSubscription;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;

    public PushSubscriptionService(PushSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void register(String userId, RegisterPushSubscriptionRequest request) {
        if (request.endpoint() == null || request.endpoint().isBlank()) {
            throw new IllegalArgumentException("Push endpoint is required");
        }
        if (request.keys() == null
                || request.keys().p256dh() == null
                || request.keys().p256dh().isBlank()
                || request.keys().auth() == null
                || request.keys().auth().isBlank()) {
            throw new IllegalArgumentException("Push subscription keys are required");
        }

        PushSubscription subscription = repository.findByUserIdAndEndpoint(userId, request.endpoint())
                .orElseGet(() -> PushSubscription.create(
                        userId,
                        request.endpoint(),
                        request.keys().p256dh(),
                        request.keys().auth()
                ));
        subscription.updateKeys(request.keys().p256dh(), request.keys().auth());
        repository.save(subscription);
    }

    @Transactional
    public void removeAllForUser(String userId) {
        repository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<PushSubscription> listForUser(String userId) {
        return repository.findByUserId(userId);
    }
}
