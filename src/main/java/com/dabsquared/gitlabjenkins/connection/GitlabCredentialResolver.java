package com.dabsquared.gitlabjenkins.connection;

import hudson.model.Item;
import hudson.model.ItemGroup;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

public class GitlabCredentialResolver {

    private Item item;
    private String credentialsId;

    public GitlabCredentialResolver() {}

    public GitlabCredentialResolver(Item item, String credentialsId) {
        this.item = item;
        this.credentialsId = credentialsId;
    }

    @Restricted(NoExternalUse.class)
    public Item getItem() {
        return item;
    }

    @Restricted(NoExternalUse.class)
    public void setItem(Item item) {
        this.item = item;
    }

    @Restricted(NoExternalUse.class)
    public String getCredentialsId() {
        return credentialsId;
    }

    @Restricted(NoExternalUse.class)
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Restricted(NoExternalUse.class)
    public static ItemGroup<?> getLookupContext(Item item) {
        return item != null ? item.getParent() : Jenkins.get();
    }

    @Restricted(NoExternalUse.class)
    public static String getLookupContextName(Item item) {
        return getLookupContext(item).getFullName();
    }
}
