package com.dabsquared.gitlabjenkins.util;

import hudson.security.ACL;
import org.acegisecurity.Authentication;

/**
 * @author Robin Müller
 */
public class ACLUtil {

    public static <T> T impersonate(Authentication auth, final Function<T> function) {
        final ObjectHolder<T> holder = new ObjectHolder<T>();
        ACL.impersonate(auth, new Runnable() {
            public void run() {
                holder.setValue(function.invoke());
            }
        });
        return holder.getValue();
    }

    public interface Function<T> {
        T invoke();
    }

    private static class ObjectHolder<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}
