package com.dabsquared.gitlabjenkins.trigger.branch;

import java.util.Collection;
import java.util.HashSet;
import org.springframework.util.AntPathMatcher;

/**
 * @author Robin Müller
 */
class AntPathMatcherSet extends HashSet<String> {

    private final transient AntPathMatcher matcher = new AntPathMatcher();

    public AntPathMatcherSet(Collection<? extends String> c) {
        super(c);
    }

    @Override
    public boolean contains(Object o) {
        for (String s : this) {
            if (matcher.match(o.toString(), s)) {
                return true;
            }
        }
        return false;
    }
}
