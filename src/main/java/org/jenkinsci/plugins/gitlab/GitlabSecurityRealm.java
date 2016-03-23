// Copyright (C) 2016, Siemens AG
// SPDX-License-Identifier:	GPL-2

package org.jenkinsci.plugins.gitlab;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationToken;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.TokenType;
import org.gitlab.api.models.GitlabUser;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Header;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.security.SecurityRealm;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;

public class GitlabSecurityRealm extends SecurityRealm {
	private static final String DEFAULT_WEB_URI = "https://gitlab.com";
	private static final String TOKEN_PATH = "/oauth/token";
	private static final String AUTHORIZE_PATH = "/oauth/authorize";
	private static final String REFERER_ATTRIBUTE = GitlabSecurityRealm.class.getName() + ".referer";
	private static final Logger LOGGER = Logger.getLogger(GitlabSecurityRealm.class.getName());

	private String gitlabUri;
	private String applicationId;
	private String secret;

	@DataBoundConstructor
	public GitlabSecurityRealm(String gitlabUri, String applicationId, String secret) {
		super();
		setGitlabUri(Util.fixEmptyAndTrim(gitlabUri));
		setApplicationId(Util.fixEmptyAndTrim(applicationId));
		setSecret(Util.fixEmptyAndTrim(secret));
	}

	public String getRedirectUrl() {
		try {
			return URLEncoder.encode(Jenkins.getInstance().getRootUrl() + "securityRealm/finishLogin", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.log(Level.WARNING, "unable to construct redirect_uri");
			throw new IllegalStateException("unable to construct redirect_uri");
		}
	}

	public String getGitlabUri() {
		return gitlabUri;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public String getSecret() {
		return secret;
	}

	private void setGitlabUri(String gitlabUri) {
		this.gitlabUri = gitlabUri;
	}

	private void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	private void setSecret(String secret) {
		this.secret = secret;
	}

	public HttpResponse doCommenceLogin(StaplerRequest request, @Header("Referer") final String referer) {
		request.getSession().setAttribute(REFERER_ATTRIBUTE, referer);
		LOGGER.log(Level.FINEST, "doCommenceLogin called");
		return HttpResponses.redirectTo(getGitlabUri() + AUTHORIZE_PATH + "?" + "client_id=" + getApplicationId()
				+ "&redirect_uri=" + getRedirectUrl() + "&scope=api" + "&response_type=code");
	}

	public HttpResponse doFinishLogin(StaplerRequest request) throws IOException {
		String code = request.getParameter("code");
		String accessToken = null;

		LOGGER.log(Level.FINEST, "doFinishLogin called and code received");

		if (code == null || code.trim().length() == 0) {
			LOGGER.log(Level.WARNING, "No code received.");
			return HttpResponses.redirectToContextRoot();
		}

		String urlParameters = "client_id=" + getApplicationId() + "&client_secret=" + secret + "&code=" + code
				+ "&grant_type=authorization_code" + "&redirect_uri=" + getRedirectUrl();
		URL url = new URL(getGitlabUri() + TOKEN_PATH);
		URLConnection conn = url.openConnection();

		conn.setDoOutput(true);
		conn.getOutputStream().write(urlParameters.getBytes(StandardCharsets.UTF_8));

		InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
		StringBuilder response = new StringBuilder();

		int data;
		while ((data = reader.read()) != -1) {
			response.append((char) data);
		}

		reader.close();

		accessToken = extractAccessToken(response.toString());

		if (accessToken != null && accessToken.trim().length() > 0) {
			LOGGER.log(Level.FINEST, "access_token received");

			GitlabAPI api = GitlabAPI.connect(gitlabUri, accessToken, TokenType.ACCESS_TOKEN);
			GitlabUser self = api.getUser();

			User u = hudson.model.User.get(self.getEmail());
			if (self.getName() != null && u.getId().equals(u.getFullName()))
				u.setFullName(self.getName());

			if (self.getEmail() != null)
				try {
					u.addProperty(new Mailer.UserProperty(self.getEmail()));
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, "can't add email to user profile.");
					return HttpResponses.redirectToContextRoot();
				}

			GrantedAuthority[] authorities = new GrantedAuthority[] { SecurityRealm.AUTHENTICATED_AUTHORITY };

			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(self.getEmail(), "",
					authorities);
			SecurityContextHolder.getContext().setAuthentication(token);
			u = User.get(token.getName());
			LOGGER.log(Level.FINEST, "GitLab user authenticated.");
		} else {
			LOGGER.log(Level.WARNING, "No access_token received.");
		}

		String referer = (String) request.getSession().getAttribute(REFERER_ATTRIBUTE);
		if (referer != null)
			return HttpResponses.redirectTo(referer);

		return HttpResponses.redirectToContextRoot();
	}

	private String extractAccessToken(String content) {
		for (String part : content.split(",")) {
			if (content.contains("access_token")) {
				String tokenParts[] = part.split(":");
				return tokenParts[1].replace("\"", "");
			}
		}
		return null;
	}

	@Override
	public boolean allowsSignup() {
		return false;
	}

	@Override
	public SecurityComponents createSecurityComponents() {
		return new SecurityComponents(new AuthenticationManager() {
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {
				if (authentication instanceof AnonymousAuthenticationToken)
					return authentication;
				throw new BadCredentialsException("Unexpected authentication type: " + authentication);
			}
		});
	}

	@Override
	public String getLoginUrl() {
		return "securityRealm/commenceLogin";
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<SecurityRealm> {

		@Override
		public String getHelpFile() {
			return "/plugin/gitlab-plugin/help/help-security-realm.html";
		}

		@Override
		public String getDisplayName() {
			return "GitLab as OAuth2 authentication service provider";
		}

		public DescriptorImpl(Class<? extends SecurityRealm> clazz) {
			super(clazz);
		}

		public DescriptorImpl() {
			super();
		}

		public String getDefaultGitlabUri() {
			return DEFAULT_WEB_URI;
		}
	}
}
