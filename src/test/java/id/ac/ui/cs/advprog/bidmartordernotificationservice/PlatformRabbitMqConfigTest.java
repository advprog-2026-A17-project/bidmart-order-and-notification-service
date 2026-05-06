package id.ac.ui.cs.advprog.bidmartordernotificationservice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PlatformRabbitMqConfigTest {

    @Test
    void applicationConfigDoesNotForceRabbitMqToLocalhostInCompose() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertFalse(properties.contains("spring.rabbitmq.addresses"));
        assertFalse(properties.contains("localhost:5672"));
        assertFalse(properties.contains("RABBITMQ_URL"));
    }
}
