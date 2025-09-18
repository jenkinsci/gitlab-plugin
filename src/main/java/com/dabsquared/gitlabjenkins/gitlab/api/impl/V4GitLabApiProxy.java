package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.V4GitLabApiProxy.ID;

import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Robin Müller.
 */
@Path("/api/" + ID)
interface V4GitLabApiProxy extends GitLabApiProxy {
    String ID = "v4";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/groups")
    @Override
    List<Group> getGroups(
            @QueryParam("all_available") Boolean allAvailable,
            @QueryParam("top_level_only") Boolean topLevelOnly,
            @QueryParam("order_by") String orderBy,
            @QueryParam("sort") String sort);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/groups/{groupId}/projects")
    @Override
    List<Project> getGroupProjects(
            @PathParam("groupId") @Encoded String groupId,
            @QueryParam("include_subgroups") Boolean includeSubgroups,
            @QueryParam("visibility") String visibility,
            @QueryParam("order_by") String orderBy,
            @QueryParam("sort") String sort);

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
            @PathParam("projectId") @Encoded Integer projectId,
            @FormParam("source_branch") String sourceBranch,
            @FormParam("target_branch") String targetBranch,
            @FormParam("title") String title);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectName}")
    @Override
    Project getProject(@PathParam("projectName") @Encoded String projectName);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}")
    @Override
    Project updateProject(
            @PathParam("projectId") @Encoded String projectId,
            @FormParam("name") String name,
            @FormParam("path") String path);

    @DELETE
    @Path("/projects/{projectId}")
    @Override
    void deleteProject(@PathParam("projectId") @Encoded String projectId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectName}/hooks")
    @Override
    List<ProjectHook> getProjectHooks(@PathParam("projectName") @Encoded String projectName);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/hooks")
    @Override
    void addProjectHook(
            @PathParam("projectId") @Encoded String projectId,
            @FormParam("url") String url,
            @FormParam("push_events") Boolean pushEvents,
            @FormParam("merge_requests_events") Boolean mergeRequestEvents,
            @FormParam("note_events") Boolean noteEvents);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/hooks")
    @Override
    void addProjectHook(
            @PathParam("projectId") @Encoded String projectId,
            @FormParam("url") String url,
            @FormParam("token") String secretToken,
            @FormParam("push_events") Boolean pushEvents,
            @FormParam("merge_requests_events") Boolean mergeRequestEvents,
            @FormParam("note_events") Boolean noteEvents);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/statuses/{sha}")
    @Override
    void changeBuildStatus(
            @PathParam("projectId") @Encoded String projectId,
            @PathParam("sha") @Encoded String sha,
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
    void changeBuildStatus(
            @PathParam("projectId") @Encoded Integer projectId,
            @PathParam("sha") @Encoded String sha,
            @FormParam("state") BuildState state,
            @FormParam("ref") String ref,
            @FormParam("context") String context,
            @FormParam("target_url") String targetUrl,
            @FormParam("description") String description);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/commits/{sha}")
    @Override
    void getCommit(@PathParam("projectId") @Encoded String projectId, @PathParam("sha") @Encoded String sha);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestIid}/merge")
    @Override
    void acceptMergeRequest(
            @PathParam("projectId") @Encoded Integer projectId,
            @PathParam("mergeRequestIid") @Encoded Integer mergeRequestIid,
            @FormParam("merge_commit_message") String mergeCommitMessage,
            @FormParam("should_remove_source_branch") Boolean shouldRemoveSourceBranch);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestIid}/notes")
    @Override
    void createMergeRequestNote(
            @PathParam("projectId") @Encoded Integer projectId,
            @PathParam("mergeRequestIid") @Encoded Integer mergeRequestIid,
            @FormParam("body") String body);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestIid}/award_emoji")
    @Override
    List<Awardable> getMergeRequestEmoji(
            @PathParam("projectId") @Encoded Integer projectId,
            @PathParam("mergeRequestIid") @Encoded Integer mergeRequestIid);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestIid}/award_emoji")
    @Override
    void awardMergeRequestEmoji(
            @PathParam("projectId") @Encoded Integer projectId,
            @PathParam("mergeRequestIid") @Encoded Integer mergeRequestIid,
            @QueryParam("name") String name);

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/projects/{projectId}/merge_requests/{mergeRequestIid}/award_emoji/{awardId}")
    @Override
    void deleteMergeRequestEmoji(
            @PathParam("projectId") @Encoded Integer projectId,
            @PathParam("mergeRequestIid") @Encoded Integer mergeRequestIid,
            @PathParam("awardId") @Encoded Integer awardId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/merge_requests")
    @Override
    List<MergeRequest> getMergeRequests(
            @PathParam("projectId") @Encoded String projectId,
            @QueryParam("state") State state,
            @QueryParam("page") int page,
            @QueryParam("per_page") int perPage);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/branches")
    @Override
    List<Branch> getBranches(@PathParam("projectId") @Encoded String projectId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/repository/branches/{branch}")
    @Override
    Branch getBranch(@PathParam("projectId") @Encoded String projectId, @PathParam("branch") @Encoded String branch);

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
    User addUser(
            @FormParam("email") String email,
            @FormParam("username") String username,
            @FormParam("name") String name,
            @FormParam("password") String password);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/users/{userId}")
    @Override
    User updateUser(
            @PathParam("userId") @Encoded String userId,
            @FormParam("email") @Encoded String email,
            @FormParam("username") String username,
            @FormParam("name") String name,
            @FormParam("password") String password);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/labels")
    @Override
    List<Label> getLabels(@PathParam("projectId") @Encoded String projectId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/pipelines")
    @Override
    List<Pipeline> getPipelines(@PathParam("projectId") @Encoded String projectId);
}
