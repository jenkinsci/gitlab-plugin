package com.dabsquared.gitlabjenkins.util;

import hudson.model.Cause;

import java.util.List;

/**
 * @author Sami Makki
 */
public class CauseUtil {

    public static <T extends Cause> T findCauseFromUpstreamCauses(List<Cause> causes, Class<T> type) {
        for (Cause cause : causes) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
            if (cause instanceof Cause.UpstreamCause) {
                List<Cause> upCauses = ((Cause.UpstreamCause) cause).getUpstreamCauses();    // Non null, returns empty list when none are set
                for (Cause upCause : upCauses) {
                    if (type.isInstance(upCause)) {
                        return type.cast(upCause);
                    }
                }
                T gitlabCause = findCauseFromUpstreamCauses(upCauses, type);
                if (gitlabCause != null) {
                    return gitlabCause;
                }
            }
        }
        return null;
    }
}
