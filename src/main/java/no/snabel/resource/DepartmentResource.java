package no.snabel.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.snabel.model.Department;
import no.snabel.model.Customer;

import java.util.List;

@Path("/api/departments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// @RolesAllowed({"USER", "ADMIN", "ACCOUNTANT"}) // Temporarily disabled for development
public class DepartmentResource extends SecureResource {

    @GET
    public Uni<List<Department>> listDepartments() {
        // For development, return all departments if no customer ID
        Long customerId = getCustomerId();
        if (customerId == null) {
            return Department.<Department>find("active = true ORDER BY code")
                    .list();
        }
        return Department.<Department>find("customer.id = ?1 and active = true ORDER BY code", customerId)
                .list();
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getDepartment(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        return Department.<Department>find("id = ?1 and customer.id = ?2", id, customerId)
                .firstResult()
                .map(department -> department == null
                    ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(department).build());
    }

    @POST
    // @RolesAllowed({"ADMIN", "ACCOUNTANT"}) // Temporarily disabled for development
    public Uni<Response> createDepartment(Department department) {
        Long customerId = getCustomerId();
        if (customerId != null) {
            department.customer = new Customer();
            department.customer.id = customerId;
        }

        return department.persistAndFlush()
                .map(d -> Response.status(Response.Status.CREATED).entity(d).build());
    }

    @PUT
    @Path("/{id}")
    // @RolesAllowed({"ADMIN", "ACCOUNTANT"}) // Temporarily disabled for development
    public Uni<Response> updateDepartment(@PathParam("id") Long id, Department updatedDepartment) {
        Long customerId = getCustomerId();
        Uni<Department> departmentQuery;
        if (customerId == null) {
            departmentQuery = Department.<Department>find("id = ?1", id).firstResult();
        } else {
            departmentQuery = Department.<Department>find("id = ?1 and customer.id = ?2", id, customerId).firstResult();
        }

        return departmentQuery.chain(department -> {
            if (department == null) {
                return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
            }
            department.code = updatedDepartment.code;
            department.name = updatedDepartment.name;
            department.description = updatedDepartment.description;
            department.active = updatedDepartment.active;
            return department.persistAndFlush()
                    .map(d -> Response.ok(d).build());
        });
    }

    @DELETE
    @Path("/{id}")
    // @RolesAllowed("ADMIN") // Temporarily disabled for development
    public Uni<Response> deleteDepartment(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        Uni<Department> departmentQuery;
        if (customerId == null) {
            departmentQuery = Department.<Department>find("id = ?1", id).firstResult();
        } else {
            departmentQuery = Department.<Department>find("id = ?1 and customer.id = ?2", id, customerId).firstResult();
        }

        return departmentQuery.chain(department -> {
            if (department == null) {
                return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
            }
            department.active = false;
            return department.persistAndFlush()
                    .map(d -> Response.noContent().build());
        });
    }
}
