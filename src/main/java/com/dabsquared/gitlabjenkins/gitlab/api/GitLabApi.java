package com.dabsquared.gitlabjenkins.gitlab.api;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Robin Müller
 */
@Path("/api/v3")
public interface GitLabApi {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects")
    Project createProject(@FormParam("name") String projectName);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests")
    MergeRequest createMergeRequest(
        @PathParam("projectId") Integer projectId,
        @FormParam("source_branch") String sourceBranch,
        @FormParam("target_branch") String targetBranch,
        @FormParam("title") String title);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectName}")
    Project getProject(@PathParam("projectName") String projectName);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}")
    Project updateProject(@PathParam("projectId") String projectId,
                          @FormParam("name") String name,
                          @FormParam("path") String path);

    @DELETE
    @Path("/projects/{projectId}")
    void deleteProject(@PathParam("projectId") String projectId);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/hooks")
    void addProjectHook(@PathParam("projectId") String projectId,
                        @FormParam("url") String url,
                        @FormParam("push_events") Boolean pushEvents,
                        @FormParam("merge_requests_events") Boolean mergeRequestEvents,
                        @FormParam("note_events") Boolean noteEvents);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/statuses/{sha}")
    void changeBuildStatus(@PathParam("projectId") String projectId,
                           @PathParam("sha") String sha,
                           @FormParam("state") BuildState state,
                           @FormParam("ref") String ref,
                           @FormParam("context") String context,
                           @FormParam("target_url") String targetUrl,
                           @FormParam("description") String description);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/statuses/{sha}")
    void changeBuildStatus(@PathParam("projectId") Integer projectId,
                           @PathParam("sha") String sha,
                           @FormParam("state") BuildState state,
                           @FormParam("ref") String ref,
                           @FormParam("context") String context,
                           @FormParam("target_url") String targetUrl,
                           @FormParam("description") String description);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/commits/{sha}")
    void getCommit(@PathParam("projectId") String projectId, @PathParam("sha") String sha);


    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestId}/merge")
    void acceptMergeRequest(@PathParam("projectId") Integer projectId,
                            @PathParam("mergeRequestId") Integer mergeRequestId,
                            @FormParam("merge_commit_message") String mergeCommitMessage,
                            @FormParam("should_remove_source_branch") boolean shouldRemoveSourceBranch);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestId}/notes")
    void createMergeRequestNote(@PathParam("projectId") Integer projectId,
                                @PathParam("mergeRequestId") Integer mergeRequestId,
                                @FormParam("body") String body);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/merge_requests")
    List<MergeRequest> getMergeRequests(@PathParam("projectId") String projectId,
                                        @QueryParam("state") State state,
                                        @QueryParam("page") int page,
                                        @QueryParam("per_page") int perPage);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/branches")
    List<Branch> getBranches(@PathParam("projectId") String projectId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/branches/{branch}")
    Branch getBranch(@PathParam("projectId") String projectId,
                     @PathParam("branch") String branch);

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    void headCurrentUser();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    User getCurrentUser();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/users")
    User addUser(@FormParam("email") String email,
                 @FormParam("username") String username,
                 @FormParam("name") String name,
                 @FormParam("password") String password);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/users/{userId}")
    User updateUser(@PathParam("userId") String userId,
                    @FormParam("email") String email,
                    @FormParam("username") String username,
                    @FormParam("name") String name,
                    @FormParam("password") String password);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/labels")
    List<Label> getLabels(@PathParam("projectId") String projectId);
}
