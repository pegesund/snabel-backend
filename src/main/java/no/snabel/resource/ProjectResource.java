package no.snabel.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.snabel.model.Project;
import no.snabel.model.Customer;

import java.util.List;

@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// @RolesAllowed({"USER", "ADMIN", "ACCOUNTANT"}) // Temporarily disabled for development
public class ProjectResource extends SecureResource {

    @GET
    public Uni<List<Project>> listProjects(
            @QueryParam("status") @DefaultValue("ACTIVE") String status) {
        Long customerId = getCustomerId();
        // For development, return all projects if no customer ID
        if (customerId == null) {
            if ("ALL".equals(status)) {
                return Project.<Project>find("active = true ORDER BY code")
                        .list();
            }
            return Project.<Project>find("active = true and status = ?1 ORDER BY code", status)
                    .list();
        }
        if ("ALL".equals(status)) {
            return Project.<Project>find("customer.id = ?1 ORDER BY code", customerId)
                    .list();
        }
        return Project.<Project>find("customer.id = ?1 and status = ?2 ORDER BY code", customerId, status)
                .list();
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getProject(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        return Project.<Project>find("id = ?1 and customer.id = ?2", id, customerId)
                .firstResult()
                .map(project -> project == null
                    ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(project).build());
    }

    @POST
    // @RolesAllowed({"ADMIN", "ACCOUNTANT"}) // Temporarily disabled for development
    public Uni<Response> createProject(Project project) {
        Long customerId = getCustomerId();
        if (customerId != null) {
            project.customer = new Customer();
            project.customer.id = customerId;
        }

        return project.persistAndFlush()
                .map(p -> Response.status(Response.Status.CREATED).entity(p).build());
    }

    @PUT
    @Path("/{id}")
    // @RolesAllowed({"ADMIN", "ACCOUNTANT"}) // Temporarily disabled for development
    public Uni<Response> updateProject(@PathParam("id") Long id, Project updatedProject) {
        Long customerId = getCustomerId();
        Uni<Project> projectQuery;
        if (customerId == null) {
            projectQuery = Project.<Project>find("id = ?1", id).firstResult();
        } else {
            projectQuery = Project.<Project>find("id = ?1 and customer.id = ?2", id, customerId).firstResult();
        }

        return projectQuery.chain(project -> {
            if (project == null) {
                return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
            }
            project.code = updatedProject.code;
            project.name = updatedProject.name;
            project.description = updatedProject.description;
            project.startDate = updatedProject.startDate;
            project.endDate = updatedProject.endDate;
            project.status = updatedProject.status;
            project.active = updatedProject.active;
            return project.persistAndFlush()
                    .map(p -> Response.ok(p).build());
        });
    }

    @DELETE
    @Path("/{id}")
    // @RolesAllowed("ADMIN") // Temporarily disabled for development
    public Uni<Response> deleteProject(@PathParam("id") Long id) {
        Long customerId = getCustomerId();
        Uni<Project> projectQuery;
        if (customerId == null) {
            projectQuery = Project.<Project>find("id = ?1", id).firstResult();
        } else {
            projectQuery = Project.<Project>find("id = ?1 and customer.id = ?2", id, customerId).firstResult();
        }

        return projectQuery.chain(project -> {
            if (project == null) {
                return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
            }
            project.active = false;
            return project.persistAndFlush()
                    .map(p -> Response.noContent().build());
        });
    }
}
