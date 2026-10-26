package com.dabsquared.gitlabjenkins.workflow.GitLabCommitStatusStep;

f = namespace(lib.FormTagLib)

f.entry(title:"Build name", field:"name") {
    f.textbox()
}

f.advanced() {
    f.optionalProperty(field: "connection", title: "Select specific GitLab Connection")

    f.entry(title:"GitLab Projects To Notify") {
        f.repeatableHeteroProperty(field: "builds", hasHeader: "true")
    }

    // Nullable Boolean: null = inherit the plugin's global default
    // (GitLabConnectionConfig#isAttachStatusToMergeRequestPipeline). This checkbox only
    // round-trips true/false; leave it alone in the UI to keep inheriting the default.
    f.entry(title:"Attach commit status to merge request pipeline", field: "attachStatusToMergeRequestPipeline") {
        f.checkbox()
    }
}





