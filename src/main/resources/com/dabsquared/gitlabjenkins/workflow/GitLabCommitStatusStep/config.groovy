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

    // Unchecked submits false (explicit opt-out); only an omitted (null) argument inherits the global default.
    f.entry(title:"Attach commit status to merge request pipeline", field: "attachStatusToMergeRequestPipeline",
            help: "/plugin/gitlab-plugin/help/help-attachStatusToMergeRequestPipeline.html") {
        f.checkbox()
    }
}





