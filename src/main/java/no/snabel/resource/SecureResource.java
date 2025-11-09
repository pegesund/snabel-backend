package no.snabel.resource;

import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

public abstract class SecureResource {

    @Inject
    JsonWebToken jwt;

    protected Long getCustomerId() {
        Object claim = jwt.getClaim("customerId");
        if (claim instanceof Number) {
            return ((Number) claim).longValue();
        }
        if (claim != null) {
            return Long.valueOf(claim.toString());
        }
        return null;
    }

    protected Long getUserId() {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number) {
            return ((Number) claim).longValue();
        }
        if (claim != null) {
            return Long.valueOf(claim.toString());
        }
        return null;
    }

    protected String getRole() {
        return jwt.getClaim("role");
    }

    protected String getUsername() {
        return jwt.getName();
    }
}
