package com.dabsquared.gitlabjenkins.gitlab.api;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;

import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Robin Müller
 */
@Path("/api/v3")
public interface GitLabApi {

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

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    void headCurrentUser();
}
