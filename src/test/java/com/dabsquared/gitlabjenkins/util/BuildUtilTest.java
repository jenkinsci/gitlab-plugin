package com.dabsquared.gitlabjenkins.util;

import org.junit.Test;

import hudson.model.Job;
import hudson.model.Run;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * @author Joshua Barker
 */

public class BuildUtilTest {

  private static final String SHA1 = "0616d12a3a24068691027a1e113147e3c1cfa2f4";
  private static final String SHALIB = "a53131154f6dfc0d1642451679fb977c5ecf31c0";
  private static final String WRONGSHA = "5f106d47c2ce17dd65774c12c0826785d16b26f7";

  public void getBuildBySHA1WithoutMergeBuilds_sha_found(){
    Job<?,?> project = createProject(SHA1);

    Run<?, ?> build = BuildUtil.getBuildBySHA1WithoutMergeBuilds(project, SHA1);
    assertThat(build, is(notNullValue()));
  }

  @Test
  public void getBuildBySHA1WithoutMergeBuilds_sha_missing(){
    Job<?,?> project = createProject(SHA1);

    Run<?, ?> build = BuildUtil.getBuildBySHA1WithoutMergeBuilds(project, WRONGSHA);
    assertThat(build, is(nullValue()));
  }

  @Test
  public void getBuildBySHA1WithoutMergeBuilds_libraryFirst(){
    Job<?,?> project = createProject(SHA1, SHALIB);

    Run<?, ?> build = BuildUtil.getBuildBySHA1WithoutMergeBuilds(project, SHA1);
    assertThat(build, is(notNullValue()));
  }

  @Test
  public void getBuildBySHA1WithoutMergeBuilds_libraryLast(){
    Job<?,?> project = createProject(SHA1, SHALIB);

    Run<?, ?> build = BuildUtil.getBuildBySHA1WithoutMergeBuilds(project, SHA1);
    assertThat(build, is(notNullValue()));
  }

  @Test
  public void getBuildBySHA1IncludingMergeBuilds_sha_found(){
    Job<?,?> project = createProject(SHA1);

    Run<?, ?> build = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, SHA1);
    assertThat(build, is(notNullValue()));
  }

  @Test
  public void getBuildBySHA1IncludingMergeBuilds_sha_missing(){
    Job<?,?> project = createProject(SHA1);

    Run<?, ?> build = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, WRONGSHA);
    assertThat(build, is(nullValue()));
  }
  
  @Test
  public void getBuildBySHA1IncludingMergeBuilds_libraryFirst(){
    Job<?,?> project = createProject(SHA1, SHALIB);

    Run<?, ?> build = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, SHA1);
    assertThat(build, is(notNullValue()));
  }

  @Test
  public void getBuildBySHA1IncludingMergeBuilds_libraryLast(){
    Job<?,?> project = createProject(SHA1, SHALIB);

    Run<?, ?> build = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, SHA1);
    assertThat(build, is(notNullValue()));
  }

  private AbstractProject<?,?> createProject(String... shas) {
    AbstractBuild build = mock(AbstractBuild.class);
    List<BuildData> buildDatas = new ArrayList<BuildData>();
    for(String sha : shas) {
      BuildData buildData = createBuildData(sha);
      buildDatas.add(buildData);
    }

    when(build.getAction(BuildData.class)).thenReturn(buildDatas.get(0));
    when(build.getActions(BuildData.class)).thenReturn(buildDatas);

    AbstractProject<?, ?> project = mock(AbstractProject.class);
    when(build.getProject()).thenReturn(project);

    RunList list = mock(RunList.class);
    when(list.iterator()).thenReturn(Arrays.asList(build).iterator());
    when(project.getBuilds()).thenReturn(list);

    return project;
  }

  private BuildData createBuildData(String sha) {
    
    BuildData buildData = mock(BuildData.class);
    Revision revision = mock(Revision.class);
    when(revision.getSha1String()).thenReturn(sha);
    when(buildData.getLastBuiltRevision()).thenReturn(revision);

    Build gitBuild = mock(Build.class);
    when(gitBuild.getMarked()).thenReturn(revision);
    when(buildData.getLastBuild(any(ObjectId.class))).thenReturn(gitBuild);
    when(gitBuild.getRevision()).thenReturn(revision);
    when(gitBuild.isFor(sha)).thenReturn(true);
    buildData.lastBuild = gitBuild;

    return buildData;
  }
}