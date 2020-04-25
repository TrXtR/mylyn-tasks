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
 *     Xiaoyang Guan - improvements
 *******************************************************************************/

package org.eclipse.mylyn.internal.jira.core.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.auth.DigestScheme;
import org.apache.commons.httpclient.auth.NTLMScheme;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.serializer.CharSetXmlWriterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.CoreUtil;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.commons.net.UnsupportedRequestException;
import org.eclipse.mylyn.commons.net.WebUtil;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.model.JiraAction;
import org.eclipse.mylyn.internal.jira.core.model.JiraAttachment;
import org.eclipse.mylyn.internal.jira.core.model.JiraComment;
import org.eclipse.mylyn.internal.jira.core.model.JiraComponent;
import org.eclipse.mylyn.internal.jira.core.model.JiraMilestone;
import org.eclipse.mylyn.internal.jira.core.model.JiraPriority;
import org.eclipse.mylyn.internal.jira.core.model.JiraRepositoryInfo;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.internal.jira.core.model.JiraSeverity;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket.Key;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketField;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketField.Type;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketResolution;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketStatus;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketType;
import org.eclipse.mylyn.internal.jira.core.model.JiraVersion;
import org.eclipse.mylyn.internal.jira.core.model.JiraWikiPage;
import org.eclipse.mylyn.internal.jira.core.model.JiraWikiPageInfo;
import org.eclipse.mylyn.internal.jira.core.util.HttpMethodInterceptor;
import org.eclipse.mylyn.internal.jira.core.util.JiraHttpClientTransportFactory;
import org.eclipse.mylyn.internal.jira.core.util.JiraHttpClientTransportFactory.JiraHttpException;
import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.internal.jira.core.util.JiraXmlRpcClientRequest;
import org.eclipse.osgi.util.NLS;

/**
 * Represents a Jira repository that is accessed through the Jira XmlRpcPlugin.
 *
 * @author Steffen Pingel
 * @author Xiaoyang Guan
 */
public class JiraXmlRpcClient extends AbstractJiraClient implements IJiraWikiClient {

	private static final Pattern ERROR_PATTERN_RPC_METHOD_NOT_FOUND = Pattern.compile("RPC method \".*\" not found"); //$NON-NLS-1$

	private static final Pattern ERROR_PATTERN_MID_AIR_COLLISION = Pattern.compile(
			"Sorry, can not save your changes.*This ticket has been modified by someone else since you started"); //$NON-NLS-1$

	private static final String ERROR_XML_RPC_PRIVILEGES_REQUIRED = "XML_RPC privileges are required to perform this operation"; //$NON-NLS-1$

	private class XmlRpcRequest {

		private final String method;

		private final Object[] parameters;

		public XmlRpcRequest(String method, Object[] parameters) {
			this.method = method;
			this.parameters = parameters;
		}

		public Object execute(IProgressMonitor monitor) throws JiraException {
			try {
				// first attempt
				return executeCallInternal(monitor);
			} catch (JiraPermissionDeniedException e) {
				if (accountMangerAuthenticationFailed) {
					// do not try again if this has failed in the past since it
					// is more likely that XML_RPC permissions have not been set
					throw e;
				}

				AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);
				if (!credentialsValid(credentials)) {
					throw e;
				}

				// try form-based authentication via AccountManagerPlugin as a
				// fall-back
				HostConfiguration hostConfiguration = WebUtil.createHostConfiguration(httpClient, location, monitor);
				try {
					authenticateAccountManager(httpClient, hostConfiguration, credentials, monitor);
				} catch (JiraLoginException loginException) {
					// caused by wrong user name or password
					throw loginException;
				} catch (IOException ignore) {
					accountMangerAuthenticationFailed = true;
					throw e;
				}

				try {
					validateAuthenticationState(httpClient);
				} catch (JiraLoginException ignore) {
					// most likely form based authentication is not supported by
					// repository
					accountMangerAuthenticationFailed = true;
					throw e;
				}

				// the authentication information is available through the shared state in httpClient
			}

			// second attempt
			return executeCallInternal(monitor);
		}

		private Object executeCallInternal(IProgressMonitor monitor) throws JiraException {
			try {
				if (isTracd && digestScheme != null) {
					probeAuthenticationScheme(monitor);
				}
				if (DEBUG_XMLRPC) {
					System.err.println(
							"Calling " + location.getUrl() + ": " + method + " " + CoreUtil.toString(parameters)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				JiraXmlRpcClientRequest request = new JiraXmlRpcClientRequest(xmlrpc.getClientConfig(), method,
						parameters, monitor);
				return xmlrpc.execute(request);
			} catch (JiraHttpException e) {
				handleAuthenticationException(e.code, e.getAuthScheme());
				// if not handled, throw generic exception
				throw new JiraException(e);
			} catch (XmlRpcException e) {
				// XXX work-around for http://jira-hacks.org/ticket/5848
				if (ERROR_XML_RPC_PRIVILEGES_REQUIRED.equals(e.getMessage()) || e.code == XML_FAULT_PERMISSION_DENIED) {
					handleAuthenticationException(HttpStatus.SC_FORBIDDEN, null);
					// should never happen as call above should always throw an exception
					throw new JiraRemoteException(e);
				} else if (isNoSuchMethodException(e)) {
					throw new JiraNoSuchMethodException(e);
				} else if (isMidAirCollision(e)) {
					throw new JiraMidAirCollisionException(e);
				} else {
					throw new JiraRemoteException(e);
				}
			} catch (OperationCanceledException e) {
				throw e;
			} catch (Exception e) {
				throw new JiraException(e);
			}
		}

		private boolean isMidAirCollision(XmlRpcException e) {
			if (e.code == XML_FAULT_GENERAL_ERROR && e.getMessage() != null
					&& ERROR_PATTERN_MID_AIR_COLLISION.matcher(e.getMessage()).find()) {
				return true;
			}
			return false;
		}

		private boolean isNoSuchMethodException(XmlRpcException e) {
			// the fault code is used for various errors, therefore detection is based on the message
			// message format by XML-RPC Plugin version:
			//  1.0.1: XML-RPC method "ticket.ge1t" not found
			//  1.0.6: RPC method "ticket.ge1t" not found
			//  1.10:  RPC method "ticket.ge1t" not found' while executing 'ticket.ge1t()
			if (e.code == XML_FAULT_GENERAL_ERROR && e.getMessage() != null
					&& ERROR_PATTERN_RPC_METHOD_NOT_FOUND.matcher(e.getMessage()).find()) {
				return true;
			}
			return false;
		}

		protected boolean handleAuthenticationException(int code, AuthScheme authScheme) throws JiraException {
			if (code == HttpStatus.SC_UNAUTHORIZED) {
				if (DEBUG_AUTH) {
					System.err.println(location.getUrl() + ": Unauthorized (" + code + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				digestScheme = null;
				JiraLoginException exception = new JiraLoginException();
				exception.setNtlmAuthRequested(authScheme instanceof NTLMScheme);
				throw exception;
			} else if (code == HttpStatus.SC_FORBIDDEN) {
				if (DEBUG_AUTH) {
					System.err.println(location.getUrl() + ": Forbidden (" + code + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				digestScheme = null;
				throw new JiraPermissionDeniedException();
			} else if (code == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
				if (DEBUG_AUTH) {
					System.err.println(location.getUrl() + ": Proxy authentication required (" + code + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				throw new JiraProxyAuthenticationException();
			} else if (code == SC_CERT_AUTH_FAILED) {
				if (DEBUG_AUTH) {
					System.err.println(location.getUrl() + ": Certificate authentication failed (" + code + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				throw new JiraSslCertificateException();
			}
			return false;
		}
	}

	private static final boolean DEBUG_XMLRPC = Boolean
			.valueOf(Platform.getDebugOption("org.eclipse.mylyn.jira.core/debug/xmlrpc")); //$NON-NLS-1$

	public static final String XMLRPC_URL = "/xmlrpc"; //$NON-NLS-1$

	public static final String REQUIRED_REVISION = "1950"; //$NON-NLS-1$

	public static final int REQUIRED_EPOCH = 0;

	public static final int REQUIRED_MAJOR = 0;

	public static final int REQUIRED_MINOR = 1;

	// XML-RPC Plugin <1.10
	private static final int XML_FAULT_GENERAL_ERROR = 1;

	// since XML-RPC Plugin 1.10
	@SuppressWarnings("unused")
	private static final int XML_FAULT_RESOURCE_NOT_FOUND = 404;

	// since XML-RPC Plugin 1.10
	private static final int XML_FAULT_PERMISSION_DENIED = 403;

	private static final int LATEST_VERSION = -1;

	public static final int REQUIRED_WIKI_RPC_VERSION = 2;

	private XmlRpcClient xmlrpc;

	private JiraHttpClientTransportFactory factory;

	private boolean accountMangerAuthenticationFailed;

	private XmlRpcClientConfigImpl config;

	private final HttpClient httpClient;

	private boolean probed;

	private volatile DigestScheme digestScheme;

	private final AuthScope authScope;

	private boolean isTracd;

	private JiraRepositoryInfo info = new JiraRepositoryInfo();

	public JiraXmlRpcClient(AbstractWebLocation location, Version version) {
		super(location, version);
		this.httpClient = createHttpClient();
		this.authScope = new AuthScope(WebUtil.getHost(repositoryUrl), WebUtil.getPort(repositoryUrl), null,
				AuthScope.ANY_SCHEME);
	}

	public synchronized XmlRpcClient getClient() throws JiraException {
		if (xmlrpc == null) {
			config = new XmlRpcClientConfigImpl();
			config.setEncoding(IJiraClient.CHARSET);
			config.setTimeZone(TimeZone.getTimeZone(IJiraClient.TIME_ZONE));
			config.setContentLengthOptional(false);
			config.setConnectionTimeout(WebUtil.getConnectionTimeout());
			config.setReplyTimeout(WebUtil.getSocketTimeout());

			xmlrpc = new XmlRpcClient();
			xmlrpc.setConfig(config);
			// bug 307200: force factory that supports proper UTF-8 encoding
			xmlrpc.setXmlWriterFactory(new CharSetXmlWriterFactory());

			factory = new JiraHttpClientTransportFactory(xmlrpc, httpClient);
			factory.setLocation(location);
			factory.setInterceptor(new HttpMethodInterceptor() {
				public void processRequest(HttpMethod method) {
					DigestScheme scheme = digestScheme;
					if (scheme != null) {
						if (DEBUG_AUTH) {
							System.err.println(location.getUrl() + ": Digest scheme is present"); //$NON-NLS-1$
						}
						Credentials creds = httpClient.getState().getCredentials(authScope);
						if (creds != null) {
							if (DEBUG_AUTH) {
								System.err.println(location.getUrl() + ": Setting digest scheme for request"); //$NON-NLS-1$
							}
							method.getHostAuthState().setAuthScheme(digestScheme);
							method.getHostAuthState().setAuthRequested(true);
						}
					}
				}

				public void processResponse(HttpMethod method) {
					AuthScheme authScheme = method.getHostAuthState().getAuthScheme();
					if (authScheme instanceof DigestScheme) {
						digestScheme = (DigestScheme) authScheme;
						if (DEBUG_AUTH) {
							System.err.println(location.getUrl() + ": Received digest scheme"); //$NON-NLS-1$
						}
					}
				}
			});
			xmlrpc.setTransportFactory(factory);

			// update configuration with latest values
			AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);
			config.setServerURL(getXmlRpcUrl(credentials));
			if (credentialsValid(credentials)) {
				Credentials httpCredentials = WebUtil.getHttpClientCredentials(credentials,
						WebUtil.getHost(location.getUrl()));
				httpClient.getState().setCredentials(authScope, httpCredentials);
//				if (CoreUtil.TEST_MODE) {
//					System.err.println(" Setting credentials: " + httpCredentials); //$NON-NLS-1$
//				}
				httpClient.getState().setCredentials(authScope, httpCredentials);
			} else {
				httpClient.getState().clearCredentials();
			}
		}

		return xmlrpc;
	}

	private URL getXmlRpcUrl(AuthenticationCredentials credentials) throws JiraException {
		try {
			String location = repositoryUrl.toString();
			if (credentialsValid(credentials)) {
				location += LOGIN_URL;
			}
			location += XMLRPC_URL;

			return new URL(location);
		} catch (Exception e) {
			throw new JiraException(e);
		}
	}

	private void probeAuthenticationScheme(IProgressMonitor monitor) throws JiraException {
		AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);
		if (!credentialsValid(credentials)) {
			return;
		}

		if (DEBUG_AUTH) {
			System.err.println(location.getUrl() + ": Probing authentication"); //$NON-NLS-1$
		}
		HostConfiguration hostConfiguration = WebUtil.createHostConfiguration(httpClient, location, monitor);
		HeadMethod method = new HeadMethod(getXmlRpcUrl(credentials).toString());
		try {
			// execute without any credentials set
			int result = WebUtil.execute(httpClient, hostConfiguration, method, new HttpState(), monitor);
			if (DEBUG_AUTH) {
				System.err.println(location.getUrl() + ": Received authentication response (" + result + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (result == HttpStatus.SC_UNAUTHORIZED || result == HttpStatus.SC_FORBIDDEN) {
				AuthScheme authScheme = method.getHostAuthState().getAuthScheme();
				if (authScheme instanceof DigestScheme) {
					this.digestScheme = (DigestScheme) authScheme;
					if (DEBUG_AUTH) {
						System.err.println(location.getUrl() + ": Received digest scheme"); //$NON-NLS-1$
					}
				} else if (authScheme instanceof BasicScheme) {
					httpClient.getParams().setAuthenticationPreemptive(true);
					if (DEBUG_AUTH) {
						System.err.println(location.getUrl() + ": Received basic scheme"); //$NON-NLS-1$
					}
				} else if (authScheme != null) {
					if (DEBUG_AUTH) {
						System.err.println(location.getUrl() + ": Received scheme (" + authScheme.getClass() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} else {
					if (DEBUG_AUTH) {
						System.err.println(location.getUrl() + ": No authentication scheme received"); //$NON-NLS-1$
					}
				}

				Header header = method.getResponseHeader("Server"); //$NON-NLS-1$
				isTracd = (header != null && header.getValue().startsWith("tracd")); //$NON-NLS-1$
				if (DEBUG_AUTH && isTracd) {
					System.err.println(location.getUrl() + ": Tracd detected"); //$NON-NLS-1$
				}

//					Header header = method.getResponseHeader("WWW-Authenticate");
//					if (header != null) {
//						if (header.getValue().startsWith("Basic")) {
//							httpClient.getParams().setAuthenticationPreemptive(true);
//						} else if (header.getValue().startsWith("Digest")) {
//							DigestScheme scheme = new DigestScheme();
//							try {
//								scheme.processChallenge(header.getValue());
//								this.digestScheme = scheme;
//							} catch (MalformedChallengeException e) {
//								// ignore
//							}
//						}
//					}
			}
		} catch (IOException e) {
			// ignore
		} finally {
			WebUtil.releaseConnection(method, monitor);
		}
	}

	private Object call(IProgressMonitor monitor, String method, Object... parameters) throws JiraException {
		monitor = Policy.monitorFor(monitor);
		JiraException lastException = null;
		for (int attempt = 0; attempt < 3; attempt++) {
			if (!probed) {
				try {
					probeAuthenticationScheme(monitor);
				} finally {
					probed = true;
				}
			}

			getClient();

			try {
				XmlRpcRequest request = new XmlRpcRequest(method, parameters);
				return request.execute(monitor);
			} catch (JiraLoginException e) {
				try {
					location.requestCredentials(AuthenticationType.REPOSITORY, null, monitor);
				} catch (UnsupportedRequestException ignored) {
					throw e;
				}
				lastException = e;
			} catch (JiraPermissionDeniedException e) {
				try {
					location.requestCredentials(AuthenticationType.REPOSITORY, null, monitor);
				} catch (UnsupportedRequestException ignored) {
					throw e;
				}
				lastException = e;
			} catch (JiraProxyAuthenticationException e) {
				try {
					location.requestCredentials(AuthenticationType.PROXY, null, monitor);
				} catch (UnsupportedRequestException ignored) {
					throw e;
				}
				lastException = e;
			} catch (JiraSslCertificateException e) {
				try {
					location.requestCredentials(AuthenticationType.CERTIFICATE, null, monitor);
				} catch (UnsupportedRequestException ignored) {
					throw e;
				}
				lastException = e;
			}
		}
		if (lastException != null) {
			throw lastException;
		} else {
			// this path should never be reached
			throw new IllegalStateException();
		}
	}

	private Object[] multicall(IProgressMonitor monitor, Map<String, Object>... calls) throws JiraException {
		Object[] result = (Object[]) call(monitor, "system.multicall", new Object[] { calls }); //$NON-NLS-1$
		for (Object item : result) {
			try {
				checkForException(item);
			} catch (XmlRpcException e) {
				throw new JiraRemoteException(e);
			} catch (Exception e) {
				throw new JiraException(e);
			}
		}
		return result;
	}

	private void checkForException(Object result) throws NumberFormatException, XmlRpcException {
		if (result instanceof Map<?, ?>) {
			Map<?, ?> exceptionData = (Map<?, ?>) result;
			if (exceptionData.containsKey("faultCode") && exceptionData.containsKey("faultString")) { //$NON-NLS-1$ //$NON-NLS-2$
				throw new XmlRpcException(Integer.parseInt(exceptionData.get("faultCode").toString()), //$NON-NLS-1$
						(String) exceptionData.get("faultString")); //$NON-NLS-1$
			} else if (exceptionData.containsKey("title")) { //$NON-NLS-1$
				String message = (String) exceptionData.get("title"); //$NON-NLS-1$
				String detail = (String) exceptionData.get("_message"); //$NON-NLS-1$
				if (detail != null) {
					message += ": " + detail; //$NON-NLS-1$
				}
				throw new XmlRpcException(XML_FAULT_GENERAL_ERROR, message);
			}
		}
	}

	private Map<String, Object> createMultiCall(String methodName, Object... parameters) throws JiraException {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("methodName", methodName); //$NON-NLS-1$
		table.put("params", parameters); //$NON-NLS-1$
		return table;
	}

	private Object getMultiCallResult(Object item) {
		return ((Object[]) item)[0];
	}

	public JiraRepositoryInfo validate(IProgressMonitor monitor) throws JiraException {
		Integer epochAPIVersion;
		Integer majorAPIVersion;
		Integer minorAPIVersion;
		try {
			Object[] result = (Object[]) call(monitor, "system.getAPIVersion"); //$NON-NLS-1$
			if (result.length >= 3) {
				epochAPIVersion = (Integer) result[0];
				majorAPIVersion = (Integer) result[1];
				minorAPIVersion = (Integer) result[2];
			} else if (result.length >= 2) {
				epochAPIVersion = 0;
				majorAPIVersion = (Integer) result[0];
				minorAPIVersion = (Integer) result[1];
			} else {
				throw new JiraException(
						NLS.bind(Messages.JiraXmlRpcClient_API_version_unsupported_Error, REQUIRED_REVISION));
			}
		} catch (JiraNoSuchMethodException e) {
			throw new JiraException(
					NLS.bind(Messages.JiraXmlRpcClient_Required_API_calls_missing_Error, REQUIRED_REVISION));
		}

		info = new JiraRepositoryInfo(epochAPIVersion, majorAPIVersion, minorAPIVersion);
		if (!info.isApiVersionOrHigher(REQUIRED_EPOCH, REQUIRED_MAJOR, REQUIRED_MINOR)) {
			throw new JiraException(NLS.bind(Messages.JiraXmlRpcClient_API_version_X_unsupported_Error, info.toString(),
					REQUIRED_REVISION));
		}
		return info;
	}

	private void updateAPIVersion(IProgressMonitor monitor) throws JiraException {
		if (info.isStale()) {
			validate(monitor);
		}
	}

	private boolean isApiVersionOrHigher(int epoch, int major, int minor, IProgressMonitor monitor)
			throws JiraException {
		updateAPIVersion(monitor);
		return info.isApiVersionOrHigher(epoch, major, minor);
	}

	public List<JiraComment> getComments(int id, IProgressMonitor monitor) throws JiraException {
		Object[] result = (Object[]) call(monitor, "ticket.changeLog", id, 0); //$NON-NLS-1$
		List<JiraComment> comments = new ArrayList<JiraComment>(result.length);
		for (Object item : result) {
			comments.add(parseChangeLogEntry((Object[]) item));
		}
		return comments;
	}

	public JiraTicket getTicket(int id, IProgressMonitor monitor) throws JiraException {
		Object[] result = (Object[]) call(monitor, "ticket.get", id); //$NON-NLS-1$
		JiraTicket ticket = parseTicket(result);

		result = (Object[]) call(monitor, "ticket.changeLog", id, 0); //$NON-NLS-1$
		for (Object item : result) {
			ticket.addComment(parseChangeLogEntry((Object[]) item));
		}

		result = (Object[]) call(monitor, "ticket.listAttachments", id); //$NON-NLS-1$
		for (Object item : result) {
			ticket.addAttachment(parseAttachment((Object[]) item));
		}

		JiraAction[] actions = getActions(id, monitor);
		ticket.setActions(actions);

		updateAttributes(new NullProgressMonitor(), false);
		JiraTicketResolution[] resolutions = getTicketResolutions();
		if (resolutions != null) {
			String[] resolutionStrings = new String[resolutions.length];
			for (int i = 0; i < resolutions.length; i++) {
				resolutionStrings[i] = resolutions[i].getName();
			}
			ticket.setResolutions(resolutionStrings);
		} else {
			ticket.setResolutions(getDefaultTicketResolutions());
		}

		return ticket;
	}

	private JiraAttachment parseAttachment(Object[] entry) {
		JiraAttachment attachment = new JiraAttachment((String) entry[0]);
		attachment.setDescription((String) entry[1]);
		attachment.setSize((Integer) entry[2]);
		attachment.setCreated(parseDate(entry[3]));
		attachment.setAuthor((String) entry[4]);
		return attachment;
	}

	private JiraComment parseChangeLogEntry(Object[] entry) {
		JiraComment comment = new JiraComment();
		comment.setCreated(parseDate(entry[0]));
		comment.setAuthor((String) entry[1]);
		comment.setField((String) entry[2]);
		comment.setOldValue((String) entry[3]);
		comment.setNewValue((String) entry[4]);
		return comment;
	}

	/* public for testing */
	@SuppressWarnings("unchecked")
	public List<JiraTicket> getTickets(int[] ids, IProgressMonitor monitor) throws JiraException {
		Map<String, Object>[] calls = new Map[ids.length];
		for (int i = 0; i < calls.length; i++) {
			calls[i] = createMultiCall("ticket.get", ids[i]); //$NON-NLS-1$
		}

		Object[] result = multicall(monitor, calls);
		assert result.length == ids.length;

		List<JiraTicket> tickets = new ArrayList<JiraTicket>(result.length);
		for (Object item : result) {
			Object[] ticketResult = (Object[]) getMultiCallResult(item);
			tickets.add(parseTicket(ticketResult));
		}

		return tickets;
	}

	public void searchForTicketIds(JiraSearch query, List<Integer> tickets, IProgressMonitor monitor)
			throws JiraException {
		// an empty query string is not valid, therefore prepend order
		Object[] result = (Object[]) call(monitor, "ticket.query", //$NON-NLS-1$
				"order=id" + query.toQuery(supportsMaxSearchResults(monitor))); //$NON-NLS-1$
		for (Object item : result) {
			tickets.add((Integer) item);
		}
	}

	@SuppressWarnings("unchecked")
	public void search(JiraSearch query, List<JiraTicket> tickets, IProgressMonitor monitor) throws JiraException {
		// an empty query string is not valid, therefore prepend order
		Object[] result = (Object[]) call(monitor, "ticket.query", //$NON-NLS-1$
				"order=id" + query.toQuery(supportsMaxSearchResults(monitor))); //$NON-NLS-1$

		Map<String, Object>[] calls = new Map[result.length];
		for (int i = 0; i < calls.length; i++) {
			calls[i] = createMultiCall("ticket.get", result[i]); //$NON-NLS-1$
		}
		result = multicall(monitor, calls);

		for (Object item : result) {
			Object[] ticketResult = (Object[]) getMultiCallResult(item);
			tickets.add(parseTicket(ticketResult));
		}
	}

	private boolean supportsWorkFlow(IProgressMonitor monitor) throws JiraException {
		return isApiVersionOrHigher(1, 0, 1, monitor);
	}

	private boolean supportsMaxSearchResults(IProgressMonitor monitor) throws JiraException {
		return isApiVersionOrHigher(1, 0, 0, monitor);
	}

	private JiraTicket parseTicket(Object[] ticketResult) throws InvalidTicketException {
		JiraTicket ticket = new JiraTicket((Integer) ticketResult[0]);
		ticket.setCreated(parseDate(ticketResult[1]));
		ticket.setLastChanged(parseDate(ticketResult[2]));
		Map<?, ?> attributes = (Map<?, ?>) ticketResult[3];
		for (Object key : attributes.keySet()) {
			ticket.putValue(key.toString(), attributes.get(key).toString());
		}
		return ticket;
	}

	private Date parseDate(Object object) {
		if (object instanceof Date) {
			return (Date) object;
		} else if (object instanceof Integer) {
			return JiraUtil.parseDate((Integer) object);
		}
		throw new ClassCastException("Unexpected object type for date: " + object.getClass()); //$NON-NLS-1$
	}

	@Override
	public synchronized void updateAttributes(IProgressMonitor monitor) throws JiraException {
		monitor.beginTask("Updating attributes", 9); //$NON-NLS-1$

		Object[] result = getAttributes("ticket.component", monitor); //$NON-NLS-1$
		data.components = new ArrayList<JiraComponent>(result.length);
		for (Object item : result) {
			data.components.add(parseComponent((Map<?, ?>) getMultiCallResult(item)));
		}
		advance(monitor, 1);

		result = getAttributes("ticket.milestone", monitor); //$NON-NLS-1$
		data.milestones = new ArrayList<JiraMilestone>(result.length);
		for (Object item : result) {
			data.milestones.add(parseMilestone((Map<?, ?>) getMultiCallResult(item)));
		}
		advance(monitor, 1);

		List<TicketAttributeResult> attributes = getTicketAttributes("ticket.priority", monitor); //$NON-NLS-1$
		data.priorities = new ArrayList<JiraPriority>(result.length);
		for (TicketAttributeResult attribute : attributes) {
			data.priorities.add(new JiraPriority(attribute.name, attribute.value));
		}
		Collections.sort(data.priorities);
		advance(monitor, 1);

		attributes = getTicketAttributes("ticket.resolution", monitor); //$NON-NLS-1$
		data.ticketResolutions = new ArrayList<JiraTicketResolution>(result.length);
		for (TicketAttributeResult attribute : attributes) {
			data.ticketResolutions.add(new JiraTicketResolution(attribute.name, attribute.value));
		}
		Collections.sort(data.ticketResolutions);
		advance(monitor, 1);

		attributes = getTicketAttributes("ticket.severity", monitor); //$NON-NLS-1$
		data.severities = new ArrayList<JiraSeverity>(result.length);
		for (TicketAttributeResult attribute : attributes) {
			data.severities.add(new JiraSeverity(attribute.name, attribute.value));
		}
		Collections.sort(data.severities);
		advance(monitor, 1);

		boolean assignValues = isApiVersionOrHigher(1, 0, 0, monitor);
		attributes = getTicketAttributes("ticket.status", assignValues, monitor); //$NON-NLS-1$
		data.ticketStatus = new ArrayList<JiraTicketStatus>(result.length);
		for (TicketAttributeResult attribute : attributes) {
			data.ticketStatus.add(new JiraTicketStatus(attribute.name, attribute.value));
		}
		Collections.sort(data.ticketStatus);
		advance(monitor, 1);

		attributes = getTicketAttributes("ticket.type", monitor); //$NON-NLS-1$
		data.ticketTypes = new ArrayList<JiraTicketType>(result.length);
		for (TicketAttributeResult attribute : attributes) {
			data.ticketTypes.add(new JiraTicketType(attribute.name, attribute.value));
		}
		Collections.sort(data.ticketTypes);
		advance(monitor, 1);

		result = getAttributes("ticket.version", monitor); //$NON-NLS-1$
		data.versions = new ArrayList<JiraVersion>(result.length);
		for (Object item : result) {
			data.versions.add(parseVersion((Map<?, ?>) getMultiCallResult(item)));
		}
		advance(monitor, 1);

		result = (Object[]) call(monitor, "ticket.getTicketFields"); //$NON-NLS-1$
		data.ticketFields = new ArrayList<JiraTicketField>(result.length);
		data.ticketFieldByName = null;
		for (Object item : result) {
			data.ticketFields.add(parseTicketField((Map<?, ?>) item));
		}
		advance(monitor, 1);
	}

	private void advance(IProgressMonitor monitor, int worked) {
		monitor.worked(worked);
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}

	}

	private JiraComponent parseComponent(Map<?, ?> result) {
		JiraComponent component = new JiraComponent((String) result.get("name")); //$NON-NLS-1$
		component.setOwner((String) result.get("owner")); //$NON-NLS-1$
		component.setDescription((String) result.get("description")); //$NON-NLS-1$
		return component;
	}

	private JiraMilestone parseMilestone(Map<?, ?> result) {
		JiraMilestone milestone = new JiraMilestone((String) result.get("name")); //$NON-NLS-1$
		milestone.setCompleted(parseDate(result.get("completed"))); //$NON-NLS-1$
		milestone.setDue(parseDate(result.get("due"))); //$NON-NLS-1$
		milestone.setDescription((String) result.get("description")); //$NON-NLS-1$
		return milestone;
	}

	private JiraVersion parseVersion(Map<?, ?> result) {
		JiraVersion version = new JiraVersion((String) result.get("name")); //$NON-NLS-1$
		version.setTime(parseDate(result.get("time"))); //$NON-NLS-1$
		version.setDescription((String) result.get("description")); //$NON-NLS-1$
		return version;
	}

	private JiraTicketField parseTicketField(Map<?, ?> result) {
		JiraTicketField field = new JiraTicketField((String) result.get("name")); //$NON-NLS-1$
		field.setType(JiraTicketField.Type.fromString((String) result.get("type"))); //$NON-NLS-1$
		field.setLabel((String) result.get("label")); //$NON-NLS-1$
		field.setDefaultValue((String) result.get("value")); //$NON-NLS-1$
		Object[] items = (Object[]) result.get("options"); //$NON-NLS-1$
		if (items != null) {
			String[] options = new String[items.length];
			for (int i = 0; i < items.length; i++) {
				options[i] = (String) items[i];
			}
			field.setOptions(options);
		}
		if (result.get("custom") != null) { //$NON-NLS-1$
			field.setCustom((Boolean) result.get("custom")); //$NON-NLS-1$
		}
		if (result.get("order") != null) { //$NON-NLS-1$
			field.setOrder((Integer) result.get("order")); //$NON-NLS-1$
		}
		if (result.get("optional") != null) { //$NON-NLS-1$
			field.setOptional((Boolean) result.get("optional")); //$NON-NLS-1$
		}
		if (result.get("width") != null) { //$NON-NLS-1$
			field.setWidth((Integer) result.get("width")); //$NON-NLS-1$
		}
		if (result.get("height") != null) { //$NON-NLS-1$
			field.setHeight((Integer) result.get("height")); //$NON-NLS-1$
		}
		return field;
	}

	@SuppressWarnings("unchecked")
	private Object[] getAttributes(String attributeType, IProgressMonitor monitor) throws JiraException {
		Object[] ids = (Object[]) call(monitor, attributeType + ".getAll"); //$NON-NLS-1$
		Map<String, Object>[] calls = new Map[ids.length];
		for (int i = 0; i < calls.length; i++) {
			calls[i] = createMultiCall(attributeType + ".get", ids[i]); //$NON-NLS-1$
		}

		Object[] result = multicall(monitor, calls);
		assert result.length == ids.length;

		return result;
	}

	private List<TicketAttributeResult> getTicketAttributes(String attributeType, IProgressMonitor monitor)
			throws JiraException {
		return getTicketAttributes(attributeType, false, monitor);
	}

	@SuppressWarnings("unchecked")
	private List<TicketAttributeResult> getTicketAttributes(String attributeType, boolean assignValues,
			IProgressMonitor monitor) throws JiraException {
		// get list of attribute ids first
		Object[] ids = (Object[]) call(monitor, attributeType + ".getAll"); //$NON-NLS-1$
		// fetch all attributes in a single call
		Map<String, Object>[] calls = new Map[ids.length];
		for (int i = 0; i < calls.length; i++) {
			calls[i] = createMultiCall(attributeType + ".get", ids[i]); //$NON-NLS-1$
		}

		Object[] result = multicall(monitor, calls);
		assert result.length == ids.length;

		List<TicketAttributeResult> attributes = new ArrayList<TicketAttributeResult>(result.length);
		for (int i = 0; i < calls.length; i++) {
			try {
				TicketAttributeResult attribute = new TicketAttributeResult();
				attribute.name = (String) ids[i];
				Object value = getMultiCallResult(result[i]);
				if (assignValues) {
					attribute.value = i;
				} else {
					attribute.value = (value instanceof Integer) ? (Integer) value : Integer.parseInt((String) value);
				}
				attributes.add(attribute);
			} catch (ClassCastException e) {
				StatusHandler.log(new Status(IStatus.WARNING, JiraCorePlugin.ID_PLUGIN,
						"Invalid response from Jira repository for attribute type: '" + attributeType + "'", e)); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (NumberFormatException e) {
				StatusHandler.log(new Status(IStatus.WARNING, JiraCorePlugin.ID_PLUGIN,
						"Invalid response from Jira repository for attribute type: '" + attributeType + "'", e)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		return attributes;
	}

	public InputStream getAttachmentData(int ticketId, String filename, IProgressMonitor monitor) throws JiraException {
		byte[] data = (byte[]) call(monitor, "ticket.getAttachment", ticketId, filename); //$NON-NLS-1$
		return new ByteArrayInputStream(data);
	}

	public void putAttachmentData(int ticketId, String filename, String description, InputStream in,
			IProgressMonitor monitor, boolean replace) throws JiraException {
		byte[] data;
		try {
			data = readData(in, new NullProgressMonitor());
		} catch (IOException e) {
			throw new JiraException(e);
		}
		call(monitor, "ticket.putAttachment", ticketId, filename, description, data, replace); //$NON-NLS-1$
	}

	private byte[] readData(InputStream in, IProgressMonitor monitor) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[512];
			while (true) {
				int count = in.read(buffer);
				if (count == -1) {
					return out.toByteArray();
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				out.write(buffer, 0, count);
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				StatusHandler
						.log(new Status(IStatus.ERROR, JiraCorePlugin.ID_PLUGIN, "Error closing attachment stream", e)); //$NON-NLS-1$
			}
		}
	}

	public void deleteAttachment(int ticketId, String filename, IProgressMonitor monitor) throws JiraException {
		call(monitor, "ticket.deleteAttachment", ticketId, filename); //$NON-NLS-1$
	}

	private class TicketAttributeResult {

		String name;

		int value;

	}

	public int createTicket(JiraTicket ticket, IProgressMonitor monitor) throws JiraException {
		Map<String, String> attributes = ticket.getValues();
		String summary = attributes.remove(Key.SUMMARY.getKey());
		String description = attributes.remove(Key.DESCRIPTION.getKey());
		if (summary == null || description == null) {
			throw new InvalidTicketException();
		}
		if (supportsNotifications(monitor)) {
			return (Integer) call(monitor, "ticket.create", summary, description, attributes, true); //$NON-NLS-1$
		} else {
			return (Integer) call(monitor, "ticket.create", summary, description, attributes); //$NON-NLS-1$
		}
	}

	private boolean supportsNotifications(IProgressMonitor monitor) throws JiraException {
		return isApiVersionOrHigher(0, 0, 2, monitor);
	}

	public void updateTicket(JiraTicket ticket, String comment, IProgressMonitor monitor) throws JiraException {
		updateAPIVersion(monitor);

		Map<String, String> attributes = ticket.getValues();
		if (!supportsWorkFlow(monitor)) {
			// submitted as part of status and resolution updates for Jira < 0.11
			attributes.remove("action"); //$NON-NLS-1$
			// avoid confusing older XML-RPC plugin versions
			attributes.remove(Key.TOKEN.getKey());
		}

		if (supportsNotifications(monitor)) {
			call(monitor, "ticket.update", ticket.getId(), comment, attributes, true); //$NON-NLS-1$
		} else {
			call(monitor, "ticket.update", ticket.getId(), comment, attributes); //$NON-NLS-1$
		}
	}

	public Set<Integer> getChangedTickets(Date since, IProgressMonitor monitor) throws JiraException {
		Object[] ids;
		ids = (Object[]) call(monitor, "ticket.getRecentChanges", since); //$NON-NLS-1$
		Set<Integer> result = new HashSet<Integer>();
		for (Object id : ids) {
			result.add((Integer) id);
		}
		return result;
	}

	public JiraAction[] getActions(int id, IProgressMonitor monitor) throws JiraException {
		if (supportsWorkFlow(monitor)) {
			Object[] actions = (Object[]) call(monitor, "ticket.getActions", id); //$NON-NLS-1$
			JiraAction[] result = new JiraAction[actions.length];
			for (int i = 0; i < result.length; i++) {
				Object[] entry = (Object[]) actions[i];
				JiraAction action = new JiraAction((String) entry[0]);
				action.setLabel((String) entry[1]);
				action.setHint((String) entry[2]);
				Object[] inputs = (Object[]) entry[3];
				// each action can be associated with fields
				for (Object inputArray : inputs) {
					Object[] inputEntry = (Object[]) inputArray;
					JiraTicketField field = new JiraTicketField((String) inputEntry[0]);
					field.setDefaultValue((String) inputEntry[1]);
					Object[] optionEntry = (Object[]) inputEntry[2];
					if (optionEntry.length == 0) {
						field.setType(Type.TEXT);
					} else {
						field.setType(Type.SELECT);
						String[] options = new String[optionEntry.length];
						for (int j = 0; j < options.length; j++) {
							options[j] = (String) optionEntry[j];
						}
						field.setOptions(options);
					}
					action.addField(field);
				}
				result[i] = action;
			}
			return result;
		} else {
			Object[] actions = (Object[]) call(monitor, "ticket.getAvailableActions", id); //$NON-NLS-1$
			JiraAction[] result = new JiraAction[actions.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = new JiraAction((String) actions[i]);
			}
			return result;
		}
	}

	public Date getTicketLastChanged(Integer id, IProgressMonitor monitor) throws JiraException {
		Object[] result = (Object[]) call(monitor, "ticket.get", id); //$NON-NLS-1$
		return parseDate(result[2]);
	}

	public void validateWikiRpcApi(IProgressMonitor monitor) throws JiraException {
		if (((Integer) call(monitor, "wiki.getRPCVersionSupported")) < 2) { //$NON-NLS-1$
			validate(monitor);
		}
	}

	public String wikiToHtml(String sourceText, IProgressMonitor monitor) throws JiraException {
		return (String) call(monitor, "wiki.wikiToHtml", sourceText); //$NON-NLS-1$
	}

	public String[] getAllWikiPageNames(IProgressMonitor monitor) throws JiraException {
		Object[] result = (Object[]) call(monitor, "wiki.getAllPages"); //$NON-NLS-1$
		String[] wikiPageNames = new String[result.length];
		for (int i = 0; i < wikiPageNames.length; i++) {
			wikiPageNames[i] = (String) result[i];
		}
		return wikiPageNames;
	}

	public JiraWikiPageInfo getWikiPageInfo(String pageName, IProgressMonitor monitor) throws JiraException {
		return getWikiPageInfo(pageName, LATEST_VERSION, null);
	}

	public JiraWikiPageInfo getWikiPageInfo(String pageName, int version, IProgressMonitor monitor)
			throws JiraException {
		// Note: if an unexpected null value is passed to XmlRpcPlugin, XmlRpcClient will throw a JiraRemoteException.
		//       So, this null-parameter checking may be omitted if resorting to XmlRpcClient is more appropriate.
		if (pageName == null) {
			throw new IllegalArgumentException("Wiki page name cannot be null"); //$NON-NLS-1$
		}

		Object result = (version == LATEST_VERSION)
				? call(monitor, "wiki.getPageInfo", pageName) // //$NON-NLS-1$
				: call(monitor, "wiki.getPageInfoVersion", pageName, version); //$NON-NLS-1$
		return parseWikiPageInfo(result);
	}

	@SuppressWarnings("unchecked")
	public JiraWikiPageInfo[] getWikiPageInfoAllVersions(String pageName, IProgressMonitor monitor)
			throws JiraException {
		JiraWikiPageInfo latestVersion = getWikiPageInfo(pageName, null);
		Map<String, Object>[] calls = new Map[latestVersion.getVersion() - 1];
		for (int i = 0; i < calls.length; i++) {
			calls[i] = createMultiCall("wiki.getPageInfoVersion", pageName, i + 1); //$NON-NLS-1$
		}

		Object[] result = multicall(monitor, calls);

		JiraWikiPageInfo[] versions = new JiraWikiPageInfo[result.length + 1];
		for (int i = 0; i < result.length; i++) {
			Object pageInfoResult = getMultiCallResult(result[i]);
			versions[i] = parseWikiPageInfo(pageInfoResult);
		}
		versions[result.length] = latestVersion;

		return versions;
	}

	private JiraWikiPageInfo parseWikiPageInfo(Object pageInfoResult) throws InvalidWikiPageException {
		// Note: Jira XML-RPC Plugin returns 0 (as Integer) if pageName or version doesn't exist,
		//       and XmlRpcClient doesn't throw an Exception in this case
		if (pageInfoResult instanceof Map<?, ?>) {
			JiraWikiPageInfo pageInfo = new JiraWikiPageInfo();
			Map<?, ?> infoMap = (Map<?, ?>) pageInfoResult;
			pageInfo.setPageName((String) infoMap.get("name")); //$NON-NLS-1$
			pageInfo.setAuthor((String) infoMap.get("author")); //$NON-NLS-1$
			pageInfo.setLastModified(parseDate(infoMap.get("lastModified"))); //$NON-NLS-1$
			pageInfo.setVersion((Integer) infoMap.get("version")); //$NON-NLS-1$
			return pageInfo;
		} else {
			throw new InvalidWikiPageException("Wiki page name or version does not exist"); //$NON-NLS-1$
		}
	}

	public String getWikiPageContent(String pageName, IProgressMonitor monitor) throws JiraException {
		return getWikiPageContent(pageName, LATEST_VERSION, null);
	}

	public String getWikiPageContent(String pageName, int version, IProgressMonitor monitor) throws JiraException {
		// Note: if an unexpected null value is passed to XmlRpcPlugin, XmlRpcClient will throw a JiraRemoteException.
		//       So, this null-parameter checking may be omitted if resorting to XmlRpcClient is more appropriate.
		if (pageName == null) {
			throw new IllegalArgumentException("Wiki page name cannot be null"); //$NON-NLS-1$
		}
		if (version == LATEST_VERSION) {
			// XmlRpcClient throws a JiraRemoteException if pageName or version doesn't exist
			return (String) call(monitor, "wiki.getPage", pageName); //$NON-NLS-1$
		} else {
			return (String) call(monitor, "wiki.getPageVersion", pageName, version); //$NON-NLS-1$
		}
	}

	public String getWikiPageHtml(String pageName, IProgressMonitor monitor) throws JiraException {
		return getWikiPageHtml(pageName, LATEST_VERSION, null);
	}

	public String getWikiPageHtml(String pageName, int version, IProgressMonitor monitor) throws JiraException {
		if (pageName == null) {
			throw new IllegalArgumentException("Wiki page name cannot be null"); //$NON-NLS-1$
		}

		if (version == LATEST_VERSION) {
			// XmlRpcClient throws a JiraRemoteException if pageName or version doesn't exist
			return (String) call(monitor, "wiki.getPageHTML", pageName); //$NON-NLS-1$
		} else {
			return (String) call(monitor, "wiki.getPageHTMLVersion", pageName, version); //$NON-NLS-1$
		}
	}

	public JiraWikiPageInfo[] getRecentWikiChanges(Date since, IProgressMonitor monitor) throws JiraException {
		if (since == null) {
			throw new IllegalArgumentException("Date parameter cannot be null"); //$NON-NLS-1$
		}

		Object[] result = (Object[]) call(monitor, "wiki.getRecentChanges", since); //$NON-NLS-1$
		JiraWikiPageInfo[] changes = new JiraWikiPageInfo[result.length];
		for (int i = 0; i < result.length; i++) {
			changes[i] = parseWikiPageInfo(result[i]);
		}
		return changes;
	}

	public JiraWikiPage getWikiPage(String pageName, IProgressMonitor monitor) throws JiraException {
		return getWikiPage(pageName, LATEST_VERSION, null);
	}

	public JiraWikiPage getWikiPage(String pageName, int version, IProgressMonitor monitor) throws JiraException {
		JiraWikiPage page = new JiraWikiPage();
		page.setPageInfo(getWikiPageInfo(pageName, version, null));
		page.setContent(getWikiPageContent(pageName, version, null));
		page.setPageHTML(getWikiPageHtml(pageName, version, null));
		return page;
	}

	public boolean putWikipage(String pageName, String content, Map<String, Object> attributes,
			IProgressMonitor monitor) throws JiraException {
		Boolean result = (Boolean) call(monitor, "wiki.putPage", pageName, content, attributes); //$NON-NLS-1$
		return result.booleanValue();
	}

	public boolean deleteWikipage(String pageName, IProgressMonitor monitor) throws JiraException {
		Boolean result = (Boolean) call(monitor, "wiki.deletePage", pageName); //$NON-NLS-1$
		return result.booleanValue();
	}

	public String[] listWikiPageAttachments(String pageName, IProgressMonitor monitor) throws JiraException {
		Object[] result = (Object[]) call(monitor, "wiki.listAttachments", pageName); //$NON-NLS-1$
		String[] attachments = new String[result.length];
		for (int i = 0; i < attachments.length; i++) {
			attachments[i] = (String) result[i];
		}
		return attachments;
	}

	public InputStream getWikiPageAttachmentData(String pageName, String fileName, IProgressMonitor monitor)
			throws JiraException {
		String attachmentName = pageName + "/" + fileName; //$NON-NLS-1$
		byte[] data = (byte[]) call(monitor, "wiki.getAttachment", attachmentName); //$NON-NLS-1$
		return new ByteArrayInputStream(data);
	}

	/**
	 * Attach a file to a Wiki page on the repository.
	 * <p>
	 * This implementation uses the wiki.putAttachmentEx() call, which provides a richer functionality specific to Jira.
	 *
	 * @param pageName
	 *            the name of the Wiki page
	 * @param fileName
	 *            the name of the file to be attached
	 * @param description
	 *            the description of the attachment
	 * @param in
	 *            An InputStream of the content of the attachment
	 * @param replace
	 *            whether to overwrite an existing attachment with the same filename
	 * @return The (possibly transformed) filename of the attachment. If <code>replace</code> is <code>true</code>, the
	 *         returned name is always the same as the argument <code>fileName</code>; if <code>replace</code> is
	 *         <code>false</code> and an attachment with name <code>fileName</code> already exists, a number is appended
	 *         to the file name (before suffix) and the generated filename of the attachment is returned.
	 * @throws JiraException
	 */
	public String putWikiPageAttachmentData(String pageName, String fileName, String description, InputStream in,
			boolean replace, IProgressMonitor monitor) throws JiraException {
		byte[] data;
		try {
			data = readData(in, new NullProgressMonitor());
		} catch (IOException e) {
			throw new JiraException(e);
		}
		return (String) call(monitor, "wiki.putAttachmentEx", pageName, fileName, description, data, replace); //$NON-NLS-1$
	}

	public void deleteTicket(int ticketId, IProgressMonitor monitor) throws JiraException {
		call(monitor, "ticket.delete", ticketId); //$NON-NLS-1$
	}

}