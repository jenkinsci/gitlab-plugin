package com.dabsquared.gitlabjenkins.workflow.pipeline

node {
	withCredentials([[
		$class: 'com.dabsquared.gitlabjenkins.connection.GitLabApiTokenBinding',
		credentialsId: "apiTokenId",
		variable: "API_TOKEN1"
	]]) {
		println "Token1 is ${API_TOKEN1.substring(1)}"
	}

	withCredentials([gitlabApiToken(
		credentialsId: "apiTokenId",
		variable: "API_TOKEN2"
	)]) {
		println "Token2 is ${API_TOKEN2.substring(1)}"
	}
}
