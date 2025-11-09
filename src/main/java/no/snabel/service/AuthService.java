package no.snabel.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.snabel.dto.LoginRequest;
import no.snabel.dto.LoginResponse;
import no.snabel.dto.TokenRequest;
import no.snabel.model.ApiClient;
import no.snabel.model.User;
import no.snabel.security.TokenService;

import java.time.LocalDateTime;

@ApplicationScoped
public class AuthService {

    @Inject
    TokenService tokenService;

    @WithTransaction
    public Uni<LoginResponse> login(LoginRequest request) {
        return User.findByUsername(request.username)
                .onItem().ifNull().failWith(() -> new SecurityException("Invalid username or password"))
                .onItem().transformToUni(user -> {
                    if (!user.active) {
                        return Uni.createFrom().failure(new SecurityException("User account is not active"));
                    }

                    if (!BcryptUtil.matches(request.password, user.passwordHash)) {
                        return Uni.createFrom().failure(new SecurityException("Invalid username or password"));
                    }

                    String deviceType = request.deviceType != null ? request.deviceType : "web";
                    String token = tokenService.generateToken(
                            user.id,
                            user.username,
                            user.customer.id,
                            user.role,
                            deviceType
                    );

                    Long expiresIn = tokenService.getTokenDuration(deviceType);

                    // Update last login
                    user.lastLogin = LocalDateTime.now();
                    return user.persistAndFlush()
                            .map(u -> new LoginResponse(
                                    token,
                                    user.id,
                                    user.username,
                                    user.customer.id,
                                    user.role,
                                    expiresIn
                            ));
                });
    }

    @WithTransaction
    public Uni<User> registerUser(User user, String plainPassword) {
        user.passwordHash = BcryptUtil.bcryptHash(plainPassword);
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user.persistAndFlush();
    }

    @WithTransaction
    public Uni<LoginResponse> clientCredentialsLogin(TokenRequest request) {
        return ApiClient.<ApiClient>find("clientId = ?1 and active = true", request.clientId)
                .firstResult()
                .onItem().ifNull().failWith(() -> new SecurityException("Invalid client credentials"))
                .onItem().transformToUni(client -> {
                    // Verify client secret
                    if (!BcryptUtil.matches(request.clientSecret, client.clientSecretHash)) {
                        return Uni.createFrom().failure(new SecurityException("Invalid client credentials"));
                    }

                    // Check expiration
                    if (client.expiresAt != null && client.expiresAt.isBefore(LocalDateTime.now())) {
                        return Uni.createFrom().failure(new SecurityException("Client credentials expired"));
                    }

                    // Generate client token
                    String token = tokenService.generateClientToken(
                            client.clientId,
                            client.customer.id,
                            client.scopes != null ? client.scopes : ""
                    );

                    Long expiresIn = tokenService.getClientTokenDuration();

                    return Uni.createFrom().item(new LoginResponse(
                            token,
                            null,  // No userId for client tokens
                            client.clientId,
                            client.customer.id,
                            "CLIENT",
                            expiresIn
                    ));
                });
    }
}
