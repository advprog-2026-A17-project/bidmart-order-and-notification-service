package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).hasText('${spring.mail.host:}')")
public class LogNotificationEmailSender implements NotificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationEmailSender.class);

    @Override
    public void sendNotificationEmail(String toEmail, String subject, String message) {
        log.info("Notification email to={} subject={} body={}", toEmail, subject, message);
    }
}
