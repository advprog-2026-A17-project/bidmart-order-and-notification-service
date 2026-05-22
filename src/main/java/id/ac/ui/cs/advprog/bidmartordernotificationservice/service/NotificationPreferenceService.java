package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getForUser(String userId) {
        return NotificationPreferenceResponse.from(findOrCreate(userId));
    }

    @Transactional
    public NotificationPreferenceResponse updateForUser(String userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference preference = findOrCreate(userId);
        if (request.email() != null) {
            preference.setEmailEnabled(request.email());
        }
        if (request.push() != null) {
            preference.setPushEnabled(request.push());
        }
        if (request.inApp() != null) {
            preference.setInAppEnabled(request.inApp());
        }
        preference.touch();
        return NotificationPreferenceResponse.from(repository.save(preference));
    }

    @Transactional(readOnly = true)
    public NotificationPreference findOrCreate(String userId) {
        return repository.findById(userId).orElseGet(() -> repository.save(NotificationPreference.defaults(userId)));
    }
}
