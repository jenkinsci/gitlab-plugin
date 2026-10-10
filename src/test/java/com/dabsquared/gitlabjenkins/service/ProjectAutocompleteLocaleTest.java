package com.dabsquared.gitlabjenkins.service;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.BranchBuilder.branch;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.LabelBuilder.label;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.trigger.branch.ProjectBranchesProvider;
import com.dabsquared.gitlabjenkins.trigger.label.ProjectLabelsProvider;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ProjectAutocompleteLocaleTest {

    @Test
    void branchAndLabelAutocompleteAreLocaleIndependent(JenkinsRule jenkins) throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            String repositoryUrl = "git@git.example.com:localeTest/Autocomplete.git";
            GitLabClientStub client = new GitLabClientStub();
            client.addBranches(
                    "localeTest/Autocomplete",
                    Arrays.asList(branch().withName("Feature/Issue-fix").build()));
            client.addLabels(
                    "localeTest/Autocomplete",
                    Arrays.asList(label().withName("Type::Issue").build()));

            GitLabConnectionProperty connection = mock(GitLabConnectionProperty.class);
            when(connection.getClient()).thenReturn(client);

            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.setScm(new GitSCM(repositoryUrl));
            project.addProperty(connection);

            assertTrue(ProjectBranchesProvider.instance()
                    .doAutoCompleteBranchesSpec(project, "issue")
                    .getValues()
                    .contains("Feature/Issue-fix"));
            assertTrue(ProjectLabelsProvider.instance()
                    .doAutoCompleteLabels(project, "issue")
                    .getValues()
                    .contains("Type::Issue"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
