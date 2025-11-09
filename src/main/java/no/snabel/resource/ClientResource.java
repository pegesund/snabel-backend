package no.snabel.resource;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.snabel.dto.CreateClientRequest;
import no.snabel.dto.CreateClientResponse;
import no.snabel.model.ApiClient;
import no.snabel.model.Customer;
import no.snabel.model.User;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.UUID;

@Path("/api/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource extends SecureResource {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @GET
    @RolesAllowed({"ADMIN"})
    @WithTransaction
    public Uni<List<ApiClient>> listClients() {
        Long customerId = getCustomerId();
        return ApiClient.find("customer.id = ?1 ORDER BY createdAt DESC", customerId).list();
    }

    @POST
    @RolesAllowed({"ADMIN"})
    @WithTransaction
    public Uni<Response> createClient(CreateClientRequest request) {
        Long customerId = getCustomerId();
        Long userId = getUserId();

        if (customerId == null || userId == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Missing customer ID or user ID")
                            .build()
            );
        }

        // Generate random secret
        String clientSecret = "secret_" + UUID.randomUUID().toString().replace("-", "");

        // Get entity references (proxies) without loading from DB
        Customer customer = new Customer();
        customer.id = customerId;

        User user = new User();
        user.id = userId;

        ApiClient client = new ApiClient();
        client.customer = customer;
        client.name = request.name;
        client.description = request.description;
        client.scopes = request.scopes;
        client.clientSecretHash = BcryptUtil.bcryptHash(clientSecret);
        client.createdBy = user;

        return client.persist()
                .map(v -> {
                    CreateClientResponse response = new CreateClientResponse(
                            client.id,
                            client.clientId,
                            clientSecret,  // Only time the secret is returned!
                            client.name,
                            client.scopes
                    );
                    return Response.status(Response.Status.CREATED).entity(response).build();
                });
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    @WithTransaction
    public Uni<Response> deleteClient(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        return ApiClient.<ApiClient>find("id = ?1 and customer.id = ?2", id, customerId)
                .firstResult()
                .chain(client -> {
                    if (client == null) {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                    }
                    // Soft delete
                    client.active = false;
                    return client.persistAndFlush()
                            .map(c -> Response.noContent().build());
                });
    }
}
