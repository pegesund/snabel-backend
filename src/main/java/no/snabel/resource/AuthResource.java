package no.snabel.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.snabel.dto.LoginRequest;
import no.snabel.dto.LoginResponse;
import no.snabel.dto.TokenRequest;
import no.snabel.service.AuthService;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    @PermitAll
    public Uni<Response> login(LoginRequest request) {
        return authService.login(request)
                .map(loginResponse -> Response.ok(loginResponse).build())
                .onFailure(SecurityException.class)
                .recoverWithItem(e -> Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse(e.getMessage()))
                        .build())
                .onFailure()
                .recoverWithItem(e -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse("An error occurred during login"))
                        .build());
    }

    @POST
    @Path("/token")
    @PermitAll
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> token(@FormParam("grant_type") String grantType,
                                @FormParam("client_id") String clientId,
                                @FormParam("client_secret") String clientSecret) {

        if (!"client_credentials".equals(grantType)) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("unsupported_grant_type"))
                    .build()
            );
        }

        TokenRequest request = new TokenRequest(grantType, clientId, clientSecret);
        return authService.clientCredentialsLogin(request)
                .map(loginResponse -> Response.ok(loginResponse).build())
                .onFailure(SecurityException.class)
                .recoverWithItem(e -> Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("invalid_client"))
                        .build())
                .onFailure()
                .recoverWithItem(e -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse("server_error"))
                        .build());
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
