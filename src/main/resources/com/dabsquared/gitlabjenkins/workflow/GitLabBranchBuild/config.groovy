package com.dabsquared.gitlabjenkins.workflow.GitLabBranchBuild;

f = namespace(lib.FormTagLib)

f.entry(title:"Build Name",field:"name") {
  f.textbox()
}

f.entry(title:"GitLab Project Id", field:"projectId") {
  f.textbox()
}

f.entry(title:"GitLab Commit sha1", field:"revisionHash") {
  f.textbox()
}

f.entry() {
  f.property(field: "connection")
}
