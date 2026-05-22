package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class LogNotificationEmailSenderTest {

    @Test
    void sendNotificationEmailLogsWithoutThrowing() {
        LogNotificationEmailSender sender = new LogNotificationEmailSender();

        sender.sendNotificationEmail("buyer@example.com", "Outbid", "You were outbid.");
    }
}
