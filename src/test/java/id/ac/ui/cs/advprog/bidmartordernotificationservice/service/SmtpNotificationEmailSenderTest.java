package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import jakarta.mail.Session;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpNotificationEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendNotificationEmailUsesBidMartTemplate() throws Exception {
        SmtpNotificationEmailSender sender = new SmtpNotificationEmailSender(mailSender, "no-reply@bidmart.test");
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        sender.sendNotificationEmail("user@example.com", "Auction won", "You won auction auction-9.");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertEquals("Auction won", sent.getSubject());
        assertTrue(sent.getAllRecipients()[0].toString().contains("user@example.com"));
        String body = extractHtmlBody(sent);
        assertTrue(body.contains("BidMart"));
        assertTrue(body.contains("You won auction auction-9."));
    }

    private String extractHtmlBody(MimeMessage message) throws Exception {
        message.saveChanges();
        if (message.isMimeType("text/html")) {
            return readContent(message.getContent());
        }
        if (message.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            StringBuilder combined = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart part = multipart.getBodyPart(index);
                combined.append(readPart(part));
            }
            return combined.toString();
        }
        return readContent(message.getContent());
    }

    private String readPart(BodyPart part) throws Exception {
        try (InputStream input = part.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readContent(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof InputStream input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return String.valueOf(content);
    }
}
