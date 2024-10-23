package com.dabsquared.gitlabjenkins.connection;

import hudson.model.Item;
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

    public Item getItem() {
        return item;
    }

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
}
