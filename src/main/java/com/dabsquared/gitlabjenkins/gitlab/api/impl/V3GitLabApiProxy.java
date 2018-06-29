package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
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

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabApiProxy.ID;


/**
 * @author Robin Müller
 */
@Path("/api/" + ID)
interface V3GitLabApiProxy extends GitLabApiProxy {
    String ID = "v3";

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects")
    @Override
    Project createProject(@FormParam("name") String projectName);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests")
    @Override
    MergeRequest createMergeRequest(
        @PathParam("projectId") Integer projectId,
        @FormParam("source_branch") String sourceBranch,
        @FormParam("target_branch") String targetBranch,
        @FormParam("title") String title);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectName}")
    @Override
    Project getProject(@PathParam("projectName") String projectName);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}")
    @Override
    Project updateProject(@PathParam("projectId") String projectId,
                          @FormParam("name") String name,
                          @FormParam("path") String path);

    @DELETE
    @Path("/projects/{projectId}")
    @Override
    void deleteProject(@PathParam("projectId") String projectId);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/hooks")
    @Override
    void addProjectHook(@PathParam("projectId") String projectId,
                        @FormParam("url") String url,
                        @FormParam("push_events") Boolean pushEvents,
                        @FormParam("merge_requests_events") Boolean mergeRequestEvents,
                        @FormParam("note_events") Boolean noteEvents);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/statuses/{sha}")
    @Override
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
    @Override
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
    @Override
    void getCommit(@PathParam("projectId") String projectId, @PathParam("sha") String sha);


    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestId}/merge")
    @Override
    void acceptMergeRequest(@PathParam("projectId") Integer projectId,
                            @PathParam("mergeRequestId") Integer mergeRequestId,
                            @FormParam("merge_commit_message") String mergeCommitMessage,
                            @FormParam("should_remove_source_branch") boolean shouldRemoveSourceBranch);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestId}/notes")
    @Override
    void createMergeRequestNote(@PathParam("projectId") Integer projectId,
                                @PathParam("mergeRequestId") Integer mergeRequestId,
                                @FormParam("body") String body);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/merge_requests")
    @Override
    List<MergeRequest> getMergeRequests(@PathParam("projectId") String projectId,
                                        @QueryParam("state") State state,
                                        @QueryParam("page") int page,
                                        @QueryParam("per_page") int perPage);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/branches")
    @Override
    List<Branch> getBranches(@PathParam("projectId") String projectId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/branches/{branch}")
    @Override
    Branch getBranch(@PathParam("projectId") String projectId,
                     @PathParam("branch") String branch);

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    @Override
    void headCurrentUser();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    @Override
    User getCurrentUser();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/users")
    @Override
    User addUser(@FormParam("email") String email,
                 @FormParam("username") String username,
                 @FormParam("name") String name,
                 @FormParam("password") String password);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/users/{userId}")
    @Override
    User updateUser(@PathParam("userId") String userId,
                    @FormParam("email") String email,
                    @FormParam("username") String username,
                    @FormParam("name") String name,
                    @FormParam("password") String password);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/labels")
    @Override
    List<Label> getLabels(@PathParam("projectId") String projectId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/pipelines")
    @Override
    List<Pipeline> getPipelines(@PathParam("projectId") String projectId);
}
