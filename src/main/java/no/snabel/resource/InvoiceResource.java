package no.snabel.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.snabel.model.Invoice;

import java.time.LocalDateTime;
import java.util.List;

@Path("/api/invoices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN", "ACCOUNTANT", "CLIENT"})
public class InvoiceResource extends SecureResource {

    @GET
    public Uni<List<Invoice>> listInvoices(
            @QueryParam("status") String status,
            @QueryParam("limit") @DefaultValue("50") int limit) {
        Long customerId = getCustomerId();

        String query = "customer.id = ?1";
        if (status != null && !status.isEmpty()) {
            query += " and status = ?2";
            return Invoice.<Invoice>find(query, customerId, status)
                    .page(0, limit)
                    .list();
        }

        return Invoice.<Invoice>find(query, customerId)
                .page(0, limit)
                .list();
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getInvoice(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        return Invoice.<Invoice>find("id = ?1 and customer.id = ?2", id, customerId)
                .firstResult()
                .map(invoice -> invoice == null
                    ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(invoice).build());
    }

    @POST
    @RolesAllowed({"ADMIN", "ACCOUNTANT"})
    public Uni<Response> createInvoice(Invoice invoice) {
        Long customerId = getCustomerId();
        Long userId = getUserId();

        invoice.customer = new no.snabel.model.Customer();
        invoice.customer.id = customerId;

        invoice.createdBy = new no.snabel.model.User();
        invoice.createdBy.id = userId;

        invoice.status = "DRAFT";
        invoice.createdAt = LocalDateTime.now();
        invoice.updatedAt = LocalDateTime.now();

        return invoice.persistAndFlush()
                .map(inv -> Response.status(Response.Status.CREATED).entity(inv).build());
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "ACCOUNTANT"})
    public Uni<Response> updateInvoice(@PathParam("id") Long id, Invoice updatedInvoice) {
        Long customerId = getCustomerId();
        return Invoice.<Invoice>find("id = ?1 and customer.id = ?2", id, customerId)
                .firstResult()
                .chain(invoice -> {
                    if (invoice == null) {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                    }
                    invoice.clientName = updatedInvoice.clientName;
                    invoice.clientOrganizationNumber = updatedInvoice.clientOrganizationNumber;
                    invoice.clientAddress = updatedInvoice.clientAddress;
                    invoice.dueDate = updatedInvoice.dueDate;
                    invoice.subtotal = updatedInvoice.subtotal;
                    invoice.vatAmount = updatedInvoice.vatAmount;
                    invoice.totalAmount = updatedInvoice.totalAmount;
                    invoice.notes = updatedInvoice.notes;
                    invoice.updatedAt = LocalDateTime.now();

                    return invoice.persistAndFlush()
                            .map(inv -> Response.ok(inv).build());
                });
    }

    @PUT
    @Path("/{id}/send")
    @RolesAllowed({"ADMIN", "ACCOUNTANT"})
    public Uni<Response> sendInvoice(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        return Invoice.<Invoice>find("id = ?1 and customer.id = ?2", id, customerId)
                .firstResult()
                .chain(invoice -> {
                    if (invoice == null) {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                    }
                    invoice.status = "SENT";
                    invoice.sentAt = LocalDateTime.now();
                    invoice.updatedAt = LocalDateTime.now();

                    return invoice.persistAndFlush()
                            .map(inv -> Response.ok(inv).build());
                });
    }

    @PUT
    @Path("/{id}/pay")
    @RolesAllowed({"ADMIN", "ACCOUNTANT"})
    public Uni<Response> markInvoicePaid(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        return Invoice.<Invoice>find("id = ?1 and customer.id = ?2", id, customerId)
                .firstResult()
                .chain(invoice -> {
                    if (invoice == null) {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                    }
                    invoice.status = "PAID";
                    invoice.paidAt = LocalDateTime.now();
                    invoice.updatedAt = LocalDateTime.now();

                    return invoice.persistAndFlush()
                            .map(inv -> Response.ok(inv).build());
                });
    }
}
