package argelbargel.jenkins.plugins.gitlab_branch_source.hooks;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;

class HookManager {
    private static final Logger LOGGER = Logger.getLogger(HookManager.class.getName());

    private final Map<String, ListenerState> managedListeners;

    HookManager() {
        managedListeners = new HashMap<>();
    }

     synchronized void addListener(GitLabSCMWebHookListener listener, boolean register) {
        addListener(listener);
        if (register) {
            registerHook(listener);
        }
    }

    synchronized void removeListener(GitLabSCMWebHookListener listener, boolean unregister) {
        removeListener(listener);
        if (unregister) {
            unregisterHook(listener);
        }
    }

    private void addListener(GitLabSCMWebHookListener listener) {
        if (!hasListener(listener.id())) {
            managedListeners.put(listener.id(), new ListenerState());
        }

        managedListeners.get(listener.id()).acquire();
    }

    private void registerHook(GitLabSCMWebHookListener listener) {
        if (!hasListener(listener.id())) {
            throw new IllegalArgumentException("unknown listener with id: " + listener.id());
        }

        ListenerState managed = managedListeners.get(listener.id());
        if (!managed.isRegistered()) {
            try {
                GitLabAPI api = gitLabAPI(listener.connectionName());
                if (listener.listensToSystem()) {
                    api.registerSystemHook(listener.url());
                } else {
                    api.registerProjectHook(listener.url(), listener.projectId());
                }
                managed.register();
            } catch (GitLabAPIException e) {
                LOGGER.warning("could not register hook " + listener.url() + ": " + e.getMessage());
            }
        }
    }

    private void removeListener(GitLabSCMWebHookListener listener) {
        ListenerState managed = managedListeners.get(listener.id());
        if (managed != null) {
            managed.release();
            if (!managed.hasUsages()) {
                unregisterAndDestroy(listener, managed);
            }
        }
    }

    private void unregisterAndDestroy(GitLabSCMWebHookListener listener, ListenerState managed) {
        unregisterHook(listener);
        if (!managed.isRegistered()) {
            managedListeners.remove(listener.id());
        }
    }

    private void unregisterHook(GitLabSCMWebHookListener listener) {
        ListenerState managed = managedListeners.get(listener.id());
        if (managed != null && managed.isRegistered()) {
            try {
                GitLabAPI api = gitLabAPI(listener.connectionName());
                if (listener.listensToSystem()) {
                    api.unregisterSystemHook(listener.url());
                } else {
                    api.unregisterProjectHook(listener.url(), listener.projectId());
                }
                managed.unregister();
            } catch (GitLabAPIException e) {
                LOGGER.warning("could not un-register hook " + listener.url() + ": " + e.getMessage());
            }
        }
    }

    boolean hasListener(String id) {
        return managedListeners.containsKey(id);
    }


    private static class ListenerState {
        private int usages;
        private boolean registered;

        ListenerState() {
            usages = 0;
            registered = false;
        }

        void acquire() {
            ++usages;
        }

        void release() {
            --usages;
        }

        boolean hasUsages() {
            return usages > 0;
        }

        void register() {
            registered = true;
        }

        void unregister() {
            registered = false;
        }

        boolean isRegistered() {
            return registered;
        }
    }
}
