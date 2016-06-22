package com.dabsquared.gitlabjenkins.testing.gitlab.rule;

import net.karneim.pojobuilder.GeneratePojoBuilder;

/**
 * @author Robin Müller
 */
public class ProjectRequest {

    private final String name;
    private final String webHookUrl;
    private final boolean pushHook;
    private final boolean mergeRequestHook;
    private final boolean noteHook;

    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public ProjectRequest(String name, String webHookUrl, boolean pushHook, boolean mergeRequestHook, boolean noteHook) {
        this.name = name;
        this.webHookUrl = webHookUrl;
        this.pushHook = pushHook;
        this.mergeRequestHook = mergeRequestHook;
        this.noteHook = noteHook;
    }

    public String getName() {
        return name;
    }

    public String getWebHookUrl() {
        return webHookUrl;
    }

    public boolean isPushHook() {
        return pushHook;
    }

    public boolean isMergeRequestHook() {
        return mergeRequestHook;
    }

    public boolean isNoteHook() {
        return noteHook;
    }
}
