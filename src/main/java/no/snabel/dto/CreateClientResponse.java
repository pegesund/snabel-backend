package no.snabel.dto;

public class CreateClientResponse {
    public Long id;
    public String clientId;
    public String clientSecret;  // Plain text - ONLY returned on creation!
    public String name;
    public String scopes;
    public String message;

    public CreateClientResponse() {
    }

    public CreateClientResponse(Long id, String clientId, String clientSecret, String name, String scopes) {
        this.id = id;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.name = name;
        this.scopes = scopes;
        this.message = "IMPORTANT: Save the client_secret now. It will not be shown again!";
    }
}
