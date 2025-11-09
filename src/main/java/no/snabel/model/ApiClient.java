package no.snabel.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_clients")
public class ApiClient extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    public Customer customer;

    @Column(name = "client_id", unique = true, nullable = false, length = 50)
    public String clientId;

    @Column(name = "client_secret_hash", nullable = false)
    public String clientSecretHash;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(length = 500)
    public String description;

    @Column(length = 500)
    public String scopes;  // Comma-separated: "read:accounts,write:invoices"

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "expires_at")
    public LocalDateTime expiresAt;

    @Column(nullable = false)
    public Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "created_by")
    public User createdBy;

    public static Uni<ApiClient> findByClientId(String clientId) {
        return find("clientId", clientId).firstResult();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (clientId == null) {
            clientId = "client_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        }
    }
}
