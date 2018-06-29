package com.dabsquared.gitlabjenkins.workflow.GitLabCommitStatusStep;

f = namespace(lib.FormTagLib)

f.entry(title:"Build name", field:"name") {
    f.textbox()
}

f.entry(title:"Gitalb connection") {
    f.property(field: "connection")
}

f.entry(title:"Gitlab Projects To Notify") {
    f.repeatableHeteroProperty(field: "builds", hasHeader: "true")
}





