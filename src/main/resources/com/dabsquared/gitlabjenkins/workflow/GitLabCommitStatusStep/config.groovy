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
}





