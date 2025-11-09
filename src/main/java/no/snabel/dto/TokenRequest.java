package no.snabel.dto;

public class TokenRequest {
    public String grantType;    // "client_credentials"
    public String clientId;
    public String clientSecret;

    public TokenRequest() {
    }

    public TokenRequest(String grantType, String clientId, String clientSecret) {
        this.grantType = grantType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
