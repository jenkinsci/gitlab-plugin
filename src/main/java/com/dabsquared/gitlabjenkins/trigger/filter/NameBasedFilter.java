package com.dabsquared.gitlabjenkins.trigger.filter;

import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    public boolean isBranchAllowed(String sourceBranchName, String targetBranchName) {
        return hasNoBranchSpecs() || (isBranchNotExcluded(targetBranchName) && isBranchIncluded(targetBranchName));
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

        return Arrays.stream(commaSeparatedString.split(",")).filter(s -> !s.isEmpty()).map(String::trim).collect(Collectors.toList());
    }
}
