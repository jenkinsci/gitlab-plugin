/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package argelbargel.jenkins.plugins.gitlab_branch_source.views;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMBranchHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.Messages;
import hudson.Extension;
import hudson.model.Actionable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.views.ViewJobFilter;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;


@SuppressWarnings("unused")
public class GitLabBranchFilter extends ViewJobFilter {
    private boolean allowMergeRequests = true;
    private boolean defaultBranchOnly = false;

    @DataBoundConstructor
    public GitLabBranchFilter() { /* NOOP */ }

    @DataBoundSetter
    public void setAllowMergeRequests(boolean value) {
        allowMergeRequests = value;
    }

    public boolean getAllowMergeRequests() {
        return allowMergeRequests;
    }

    @DataBoundSetter
    public void setDefaultBranchOnly(boolean value) {
        defaultBranchOnly = value;
    }

    public boolean getDefaultBranchOnly() {
        return defaultBranchOnly;
    }


    @Override
    public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView) {
        for (TopLevelItem item : all) {
            if (added.contains(item)) {
                continue;
            }

            SCMHead head = SCMHead.HeadByItem.findHead(item);
            if (head instanceof GitLabSCMBranchHead && filter(item) && filter((GitLabSCMBranchHead) head)) {
                added.add(item);
            }
        }
        return added;
    }

    private boolean filter(Item item) {
        return !(item instanceof Actionable) || !defaultBranchOnly || ((Actionable) item).getAction(PrimaryInstanceMetadataAction.class) != null;
    }

    private boolean filter(GitLabSCMBranchHead head) {
        return allowMergeRequests || !head.hasMergeRequest();
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return Messages.GitLabSCMViewFilter_Branch_DisplayName();
        }
    }
}
