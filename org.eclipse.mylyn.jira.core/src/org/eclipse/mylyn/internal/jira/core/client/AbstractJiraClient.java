/*******************************************************************************
 * Copyright (c) 2006, 2010 Steffen Pingel and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Steffen Pingel - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.jira.core.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.mylyn.commons.core.CoreUtil;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.WebUtil;
import org.eclipse.mylyn.internal.jira.core.model.JiraComponent;
import org.eclipse.mylyn.internal.jira.core.model.JiraMilestone;
import org.eclipse.mylyn.internal.jira.core.model.JiraPriority;
import org.eclipse.mylyn.internal.jira.core.model.JiraSeverity;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketField;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketResolution;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketStatus;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketType;
import org.eclipse.mylyn.internal.jira.core.model.JiraVersion;

/**
 * @author Steffen Pingel
 */
public abstract class AbstractJiraClient implements IJiraClient {

	/**
	 * Artificial status code to indicate that SSL cert authentication failed.
	 */
	public static final int SC_CERT_AUTH_FAILED = 499;

	protected static final boolean DEBUG_AUTH = Boolean
			.valueOf(Platform.getDebugOption("org.eclipse.mylyn.jira.core/debug/authentication")); //$NON-NLS-1$

	protected static final String USER_AGENT = "JiraConnector"; //$NON-NLS-1$

	private static final String LOGIN_COOKIE_NAME = "jira_auth"; //$NON-NLS-1$

	protected final String repositoryUrl;

	protected final Version version;

	protected final AbstractWebLocation location;

	protected JiraClientData data;

	public AbstractJiraClient(URL repositoryUrl, Version version, String username, String password, Proxy proxy) {
		this.repositoryUrl = repositoryUrl.toString();
		this.version = version;

		this.location = null;

		this.data = new JiraClientData();
	}

	public AbstractJiraClient(AbstractWebLocation location, Version version) {
		this.location = location;
		this.version = version;
		this.repositoryUrl = location.getUrl();

		this.data = new JiraClientData();
	}

	protected HttpClient createHttpClient() {
		HttpClient httpClient = new HttpClient();
		httpClient.setHttpConnectionManager(WebUtil.getConnectionManager());
		httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
		WebUtil.configureHttpClient(httpClient, USER_AGENT);
		return httpClient;
	}

	public Version getAccessMode() {
		return version;
	}

	protected boolean credentialsValid(AuthenticationCredentials credentials) {
		return credentials != null && credentials.getUserName().length() > 0;
	}

	protected void authenticateAccountManager(HttpClient httpClient, HostConfiguration hostConfiguration,
			AuthenticationCredentials credentials, IProgressMonitor monitor) throws IOException, JiraLoginException {
		String formToken = getFormToken(httpClient);
		authenticateAccountManagerInternal(httpClient, hostConfiguration, credentials, monitor, formToken);

		// form token is based on a cookie which may have changed due to the login post
		// re-try with new token to ensure we are logging in with the right token, otherwise user won't be logged in
		String newFormToken = getFormToken(httpClient);
		if (formToken.length() == 0 && !formToken.equals(newFormToken)) {
			authenticateAccountManagerInternal(httpClient, hostConfiguration, credentials, monitor, newFormToken);
		}
	}

	public void authenticateAccountManagerInternal(HttpClient httpClient, HostConfiguration hostConfiguration,
			AuthenticationCredentials credentials, IProgressMonitor monitor, String formToken)
			throws IOException, JiraLoginException {
		PostMethod post = new PostMethod(WebUtil.getRequestPath(repositoryUrl + LOGIN_URL));
		post.setFollowRedirects(false);
		NameValuePair[] data = { new NameValuePair("referer", ""), //$NON-NLS-1$ //$NON-NLS-2$
				new NameValuePair("user", credentials.getUserName()), //$NON-NLS-1$
				new NameValuePair("password", credentials.getPassword()), //$NON-NLS-1$
				new NameValuePair("__FORM_TOKEN", formToken) }; //$NON-NLS-1$

		post.setRequestBody(data);
		try {
			if (DEBUG_AUTH) {
				System.err.println(location.getUrl() + ": Attempting form-based account manager authentication"); //$NON-NLS-1$
			}
			int code = WebUtil.execute(httpClient, hostConfiguration, post, monitor);
			if (DEBUG_AUTH) {
				System.err.println(location.getUrl() + ": Received account manager response (" + code + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// code should be a redirect in case of success
			if (code == HttpURLConnection.HTTP_OK) {
				throw new JiraLoginException();
			}
		} finally {
			WebUtil.releaseConnection(post, monitor);
		}
	}

	private String getFormToken(HttpClient httpClient) {
		Cookie[] cookies = httpClient.getState().getCookies();
		for (Cookie cookie : cookies) {
			if ("jira_form_token".equals(cookie.getName())) { //$NON-NLS-1$
				return cookie.getValue();
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Check if authentication cookie has been set.
	 *
	 * @throws JiraLoginException
	 *             thrown if the cookie has not been set
	 */
	protected void validateAuthenticationState(HttpClient httpClient) throws JiraLoginException {
		Cookie[] cookies = httpClient.getState().getCookies();
		for (Cookie cookie : cookies) {
			if (LOGIN_COOKIE_NAME.equals(cookie.getName())) {
				return;
			}
		}

		if (CoreUtil.TEST_MODE) {
			System.err.println(" Authentication failed: " + httpClient.getState()); //$NON-NLS-1$
		}

		throw new JiraLoginException();
	}

	public JiraComponent[] getComponents() {
		return (data.components != null) ? data.components.toArray(new JiraComponent[0]) : null;
	}

	public JiraMilestone[] getMilestones() {
		return (data.milestones != null) ? data.milestones.toArray(new JiraMilestone[0]) : null;
	}

	public JiraPriority[] getPriorities() {
		return (data.priorities != null) ? data.priorities.toArray(new JiraPriority[0]) : null;
	}

	public JiraSeverity[] getSeverities() {
		return (data.severities != null) ? data.severities.toArray(new JiraSeverity[0]) : null;
	}

	public JiraTicketField[] getTicketFields() {
		return (data.ticketFields != null) ? data.ticketFields.toArray(new JiraTicketField[0]) : null;
	}

	public JiraTicketField getTicketFieldByName(String name) {
		if (data.ticketFields != null) {
			synchronized (this) {
				// lazily fill fieldByName map
				if (data.ticketFieldByName == null) {
					data.ticketFieldByName = new HashMap<String, JiraTicketField>();
					for (JiraTicketField field : data.ticketFields) {
						data.ticketFieldByName.put(field.getName(), field);
					}
				}
				return data.ticketFieldByName.get(name);
			}
		}
		return null;
	}

	public JiraTicketResolution[] getTicketResolutions() {
		return (data.ticketResolutions != null) ? data.ticketResolutions.toArray(new JiraTicketResolution[0]) : null;
	}

	public JiraTicketStatus[] getTicketStatus() {
		return (data.ticketStatus != null) ? data.ticketStatus.toArray(new JiraTicketStatus[0]) : null;
	}

	public JiraTicketType[] getTicketTypes() {
		return (data.ticketTypes != null) ? data.ticketTypes.toArray(new JiraTicketType[0]) : null;
	}

	public JiraVersion[] getVersions() {
		return (data.versions != null) ? data.versions.toArray(new JiraVersion[0]) : null;
	}

	public boolean hasAttributes() {
		return (data.lastUpdate != 0);
	}

	public void updateAttributes(IProgressMonitor monitor, boolean force) throws JiraException {
		if (!hasAttributes() || force) {
			updateAttributes(monitor);
			data.lastUpdate = System.currentTimeMillis();
		}
	}

	public abstract void updateAttributes(IProgressMonitor monitor) throws JiraException;

	public void setData(JiraClientData data) {
		this.data = data;
	}

	public String[] getDefaultTicketResolutions() {
		return new String[] { "fixed", "invalid", "wontfix", "duplicate", "worksforme" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	public String[] getDefaultTicketActions(String status) {
		if ("new".equals(status)) { //$NON-NLS-1$
			return new String[] { "leave", "resolve", "reassign", "accept" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} else if ("assigned".equals(status)) { //$NON-NLS-1$
			return new String[] { "leave", "resolve", "reassign" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else if ("reopened".equals(status)) { //$NON-NLS-1$
			return new String[] { "leave", "resolve", "reassign" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else if ("closed".equals(status)) { //$NON-NLS-1$
			return new String[] { "leave", "reopen" }; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	public String getUrl() {
		return repositoryUrl;
	}

}
