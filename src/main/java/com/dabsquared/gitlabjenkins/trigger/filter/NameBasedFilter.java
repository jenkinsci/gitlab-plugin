package com.dabsquared.gitlabjenkins.trigger.filter;

import com.google.common.base.Splitter;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Robin Müller
 */
class NameBasedFilter implements BranchFilter {

    private final List<String> includedBranches;
    private final List<String> excludedBranches;

    public NameBasedFilter(String includedBranches, String excludedBranches) {
        this.includedBranches = convert(includedBranches);
        this.excludedBranches = convert(excludedBranches);
    }

    @Override
    public boolean isBranchAllowed(String branchName) {
        return hasNoBranchSpecs() || (isBranchNotExcluded(branchName) && isBranchIncluded(branchName));
    }

    private boolean hasNoBranchSpecs() {
        return includedBranches.isEmpty() && excludedBranches.isEmpty();
    }

    private boolean isBranchNotExcluded(String branchName) {
        AntPathMatcher matcher = new AntPathMatcher();
        for (String excludePattern : excludedBranches) {
            if (matcher.match(excludePattern, branchName)) {
                return false;
            }
        }
        return true;
    }

    private boolean isBranchIncluded(String branchName) {
        AntPathMatcher matcher = new AntPathMatcher();
        for (String includePattern : includedBranches) {
            if (matcher.match(includePattern, branchName)) {
                return true;
            }
        }
        return includedBranches.isEmpty();
    }

    private List<String> convert(String commaSeparatedString) {
        if (commaSeparatedString == null)
            return Collections.EMPTY_LIST;

        ArrayList<String> result = new ArrayList<>();
        for (String s : Splitter.on(',').omitEmptyStrings().trimResults().split(commaSeparatedString)) {
            result.add(s);
        }
        return result;
    }
}
