package id.ac.ui.cs.advprog.bidmartordernotificationservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Binds STOMP Principal to JWT subject and blocks cross-user topic subscriptions.
 */
@Component
public class NotificationStompAuthorizationInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(NotificationStompAuthorizationInterceptor.class);
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final Pattern USER_NOTIFICATION_TOPIC =
            Pattern.compile("^/topic/notifications/users/([^/]+)$");
    private static final Pattern USER_QUEUE = Pattern.compile("^/user/([^/]+)/queue/notifications$");

    private final SecretKey signingKey;

    public NotificationStompAuthorizationInterceptor(
            @Value("${bidmart.auth.jwt-secret}") String jwtSecret
    ) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            bindPrincipalFromBearer(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();
            if (principal != null && destination != null && !isDestinationAllowed(principal.getName(), destination)) {
                throw new IllegalArgumentException("Subscription denied for destination: " + destination);
            }
        }

        return message;
    }

    private void bindPrincipalFromBearer(StompHeaderAccessor accessor) {
        String authorization = firstNativeHeader(accessor, "Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(authorization.substring("Bearer ".length()))
                    .getPayload();
            if (!TOKEN_TYPE_ACCESS.equals(claims.get("type", String.class))) {
                return;
            }
            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                return;
            }
            accessor.setUser(new StompUserPrincipal(userId));
        } catch (JwtException ex) {
            // Anonymous connect remains possible for public auction topics; user queues require Principal.
            log.debug("STOMP CONNECT rejected bearer token: {}", ex.getMessage());
        }
    }

    @Nullable
    private String firstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }

    boolean isDestinationAllowed(String principalUserId, String destination) {
        var userTopic = USER_NOTIFICATION_TOPIC.matcher(destination);
        if (userTopic.matches()) {
            return principalUserId.equals(userTopic.group(1));
        }
        var userQueue = USER_QUEUE.matcher(destination);
        if (userQueue.matches()) {
            return principalUserId.equals(userQueue.group(1));
        }
        // Public auction broadcast topics are readable by any connected client.
        return destination.startsWith("/topic/auctions")
                || destination.startsWith("/topic/listings/")
                || destination.startsWith("/topic/sellers/");
    }

    private record StompUserPrincipal(String userId) implements Principal {
        @Override
        public String getName() {
            return userId;
        }
    }
}
