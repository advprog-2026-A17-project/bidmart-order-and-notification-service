package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Component
@Profile("!local")
public class SmtpNotificationEmailSender implements NotificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationEmailSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpNotificationEmailSender(
            JavaMailSender mailSender,
            @Value("${bidmart.notification.email.from:${AUTH_EMAIL_VERIFICATION_FROM:no-reply@bidmart.local}}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    @Override
    public void sendNotificationEmail(String toEmail, String subject, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(message, htmlBody(subject, message));
            mailSender.send(mimeMessage);
            log.info("Notification email sent to {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send notification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String htmlBody(String subject, String message) {
        String safeSubject = HtmlUtils.htmlEscape(subject);
        String safeMessage = HtmlUtils.htmlEscape(message).replace("\n", "<br>");
        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f7f8fa;font-family:Inter,Segoe UI,Arial,sans-serif;color:#111820;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f7f8fa;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #d9dde3;border-radius:14px;overflow:hidden;">
                          <tr>
                            <td style="padding:24px 24px 16px;border-bottom:1px solid #d9dde3;">
                              <div style="font-size:13px;font-weight:700;color:#0064d2;letter-spacing:.04em;text-transform:uppercase;">BidMart</div>
                              <h1 style="margin:8px 0 0;font-size:22px;line-height:1.25;">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px;">
                              <p style="margin:0;font-size:15px;line-height:1.6;color:#334155;">%s</p>
                              <p style="margin:20px 0 0;font-size:12px;line-height:1.6;color:#5f6b7a;">Sign in to BidMart to view details in your notification center.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeSubject, safeMessage);
    }
}
