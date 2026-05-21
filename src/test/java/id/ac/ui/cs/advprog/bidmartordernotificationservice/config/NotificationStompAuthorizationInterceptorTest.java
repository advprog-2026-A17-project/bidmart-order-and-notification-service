package id.ac.ui.cs.advprog.bidmartordernotificationservice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationStompAuthorizationInterceptorTest {

    private NotificationStompAuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new NotificationStompAuthorizationInterceptor(
                "bidmart-auth-secret-key-bidmart-auth-secret-key"
        );
    }

    @Test
    void shouldAllowOwnNotificationTopic() {
        assertTrue(interceptor.isDestinationAllowed(
                "buyer-1",
                "/topic/notifications/users/buyer-1"
        ));
    }

    @Test
    void shouldDenyAnotherUsersNotificationTopic() {
        assertFalse(interceptor.isDestinationAllowed(
                "buyer-1",
                "/topic/notifications/users/buyer-2"
        ));
    }

    @Test
    void shouldAllowPublicAuctionTopics() {
        assertTrue(interceptor.isDestinationAllowed("buyer-1", "/topic/auctions"));
        assertTrue(interceptor.isDestinationAllowed("buyer-1", "/topic/auctions/auction-9"));
        assertTrue(interceptor.isDestinationAllowed("buyer-1", "/topic/listings/listing-9"));
    }

    @Test
    void preSendConnectShouldBindPrincipalFromAccessToken() {
        String secret = "bidmart-auth-secret-key-bidmart-auth-secret-key";
        String token = Jwts.builder()
                .subject("buyer-1")
                .claim("type", "access")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, noopChannel());

        assertNotNull(result);
        Principal principal = accessor.getUser();
        assertNotNull(principal);
        assertEquals("buyer-1", principal.getName());
    }

    @Test
    void preSendSubscribeShouldRejectForeignUserTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> "buyer-1");
        accessor.setDestination("/topic/notifications/users/buyer-2");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, noopChannel())
        );
    }

    private static MessageChannel noopChannel() {
        return new MessageChannel() {
            @Override
            public boolean send(Message<?> message) {
                return true;
            }

            @Override
            public boolean send(Message<?> message, long timeout) {
                return true;
            }
        };
    }
}
