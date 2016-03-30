package com.dabsquared.gitlabjenkins.gitlab.api;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Robin Müller
 */
@Path("/api/v3")
public interface GitLabApi {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects")
    Project createProject(@QueryParam("name") String projectName);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/merge_requests")
    void createMergeRequest(
        @PathParam("projectId") Integer projectId,
        @QueryParam("source_branch") String sourceBranch,
        @QueryParam("target_branch") String targetBranch,
        @QueryParam("title") String title);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectName}")
    Project getProject(@PathParam("projectName") String projectName);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}")
    Project updateProject(@PathParam("projectId") String projectId,
                          @QueryParam("name") String name,
                          @QueryParam("path") String path);

    @DELETE
    @Path("/projects/{projectId}")
    void deleteProject(@PathParam("projectId") String projectId);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/hooks")
    void addProjectHook(@PathParam("projectId") String projectId,
                        @QueryParam("url") String url,
                        @QueryParam("push_events") Boolean pushEvents,
                        @QueryParam("merge_request_events") Boolean mergeRequestEvents);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/statuses/{sha}")
    void changeBuildStatus(@PathParam("projectId") String projectId,
                           @PathParam("sha") String sha,
                           @QueryParam("state") BuildState state,
                           @QueryParam("ref") String ref,
                           @QueryParam("context") String context,
                           @QueryParam("target_url") String targetUrl,
                           @QueryParam("description") String description);

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/commits/{sha}")
    void headCommit(@PathParam("projectId") String projectId, @PathParam("sha") String sha);


    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/merge_request/{mergeRequestId}/merge")
    void acceptMergeRequest(@PathParam("projectId") String projectId,
                            @PathParam("mergeRequestId") Integer mergeRequestId,
                            @QueryParam("merge_commit_message") String mergeCommitMessage,
                            @QueryParam("should_remove_source_branch") boolean shouldRemoveSourceBranch);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/merge_request/{mergeRequestId}/comments")
    void createMergeRequestNote(@PathParam("projectId") String projectId,
                                @PathParam("mergeRequestId") Integer mergeRequestId,
                                @QueryParam("note") String note);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/merge_requests")
    Response getMergeRequests(@PathParam("projectId") String projectId,
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
    @Path("/users")
    User addUser(@QueryParam("email") String email,
                 @QueryParam("username") String username,
                 @QueryParam("name") String name,
                 @QueryParam("password") String password);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/users/{userId}")
    User updateUser(@PathParam("userId") String userId,
                    @QueryParam("email") String email,
                    @QueryParam("username") String username,
                    @QueryParam("name") String name,
                    @QueryParam("password") String password);
}
