package com.dabsquared.gitlabjenkins.trigger.branch;

import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Robin Müller
 */
class AntPathMatcherSet extends HashSet<String> {

    public AntPathMatcherSet(Collection<? extends String> c) {
        super(c);
    }

    private transient final AntPathMatcher matcher = new AntPathMatcher();

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
