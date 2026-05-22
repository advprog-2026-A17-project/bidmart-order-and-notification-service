package id.ac.ui.cs.advprog.bidmartordernotificationservice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformRabbitMqConfigTest {

    @Test
    void applicationConfigDoesNotForceRabbitMqToLocalhostInCompose() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertTrue(properties.contains("SPRING_RABBITMQ_URL"));
        assertTrue(properties.contains("CLOUDAMQP_URL"));
        assertFalse(properties.contains("localhost:5672"));
    }
}
