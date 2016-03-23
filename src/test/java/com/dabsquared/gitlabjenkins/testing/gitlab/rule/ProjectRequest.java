package com.dabsquared.gitlabjenkins.testing.gitlab.rule;

import net.karneim.pojobuilder.GeneratePojoBuilder;

/**
 * @author Robin Müller
 */
public class ProjectRequest {

    private final String name;
    private final String webHookUrl;
    private final boolean pushHock;
    private final boolean mergeRequestHook;

    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public ProjectRequest(String name, String webHookUrl, boolean pushHock, boolean mergeRequestHook) {
        this.name = name;
        this.webHookUrl = webHookUrl;
        this.pushHock = pushHock;
        this.mergeRequestHook = mergeRequestHook;
    }

    public String getName() {
        return name;
    }

    public String getWebHookUrl() {
        return webHookUrl;
    }

    public boolean isPushHock() {
        return pushHock;
    }

    public boolean isMergeRequestHook() {
        return mergeRequestHook;
    }
}
