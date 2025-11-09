package no.snabel.dto;

public class CreateClientRequest {
    public String name;
    public String description;
    public String scopes;  // Comma-separated

    public CreateClientRequest() {
    }

    public CreateClientRequest(String name, String description, String scopes) {
        this.name = name;
        this.description = description;
        this.scopes = scopes;
    }
}
