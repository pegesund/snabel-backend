package no.snabel.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "snabel.jwt.duration.web", defaultValue = "86400")
    Long webTokenDuration;

    @ConfigProperty(name = "snabel.jwt.duration.app", defaultValue = "2592000")
    Long appTokenDuration;

    @ConfigProperty(name = "snabel.jwt.duration.client", defaultValue = "3600")
    Long clientTokenDuration;  // 1 hour for client credentials

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "https://snabel.no")
    String issuer;

    public String generateToken(Long userId, String username, Long customerId, String role, String deviceType) {
        long duration = "app".equalsIgnoreCase(deviceType) ? appTokenDuration : webTokenDuration;

        return Jwt.issuer(issuer)
                .upn(username)
                .claim("userId", userId)
                .claim("customerId", customerId)
                .claim("role", role)
                .claim("deviceType", deviceType)
                .claim("tokenType", "user")
                .groups(Set.of(role))
                .expiresIn(Duration.ofSeconds(duration))
                .sign();
    }

    public String generateClientToken(String clientId, Long customerId, String scopes) {
        return Jwt.issuer(issuer)
                .upn(clientId)  // Use clientId as principal
                .claim("clientId", clientId)
                .claim("customerId", customerId)
                .claim("scopes", scopes)
                .claim("tokenType", "client")
                .groups(Set.of("CLIENT"))  // Special group for client tokens
                .expiresIn(Duration.ofSeconds(clientTokenDuration))
                .sign();
    }

    public Long getTokenDuration(String deviceType) {
        return "app".equalsIgnoreCase(deviceType) ? appTokenDuration : webTokenDuration;
    }

    public Long getClientTokenDuration() {
        return clientTokenDuration;
    }
}
