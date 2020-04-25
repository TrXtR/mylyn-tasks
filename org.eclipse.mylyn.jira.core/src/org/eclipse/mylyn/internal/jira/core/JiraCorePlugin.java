/*******************************************************************************
 * Copyright (c) 2006, 2008 Steffen Pingel and others.
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

package org.eclipse.mylyn.internal.jira.core;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.mylyn.internal.jira.core.client.InvalidTicketException;
import org.eclipse.mylyn.internal.jira.core.client.JiraException;
import org.eclipse.mylyn.internal.jira.core.client.JiraLoginException;
import org.eclipse.mylyn.internal.jira.core.client.JiraMidAirCollisionException;
import org.eclipse.mylyn.internal.jira.core.client.JiraPermissionDeniedException;
import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.osgi.framework.BundleContext;

/**
 * The headless Jira plug-in class.
 * 
 * @author Steffen Pingel
 */
public class JiraCorePlugin extends Plugin {

	public static final String ID_PLUGIN = "org.eclipse.mylyn.jira.core"; //$NON-NLS-1$

	public static final String ENCODING_UTF_8 = "UTF-8"; //$NON-NLS-1$

	private static JiraCorePlugin plugin;

	public final static String CONNECTOR_KIND = "jira"; //$NON-NLS-1$

	private JiraRepositoryConnector connector;

	public JiraCorePlugin() {
	}

	public static JiraCorePlugin getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (connector != null) {
			connector.stop();
			connector = null;
		}

		plugin = null;
		super.stop(context);
	}

	public JiraRepositoryConnector getConnector() {
		return connector;
	}

	void setConnector(JiraRepositoryConnector connector) {
		this.connector = connector;
	}

	/**
	 * Returns the path to the file caching repository attributes.
	 */
	protected IPath getRepostioryAttributeCachePath() {
		IPath stateLocation = Platform.getStateLocation(getBundle());
		IPath cacheFile = stateLocation.append("repositoryConfigurations"); //$NON-NLS-1$
		return cacheFile;
	}

	public static IStatus toStatus(Throwable e, TaskRepository repository) {
		if (e instanceof JiraLoginException) {
			return RepositoryStatus.createLoginError(repository.getRepositoryUrl(), ID_PLUGIN);
		} else if (e instanceof JiraPermissionDeniedException) {
			return JiraUtil.createPermissionDeniedError(repository.getRepositoryUrl(), ID_PLUGIN);
		} else if (e instanceof InvalidTicketException) {
			return new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR, ID_PLUGIN,
					RepositoryStatus.ERROR_IO, Messages.JiraCorePlugin_the_SERVER_RETURNED_an_UNEXPECTED_RESOPNSE, e);
		} else if (e instanceof JiraMidAirCollisionException) {
			return RepositoryStatus.createCollisionError(repository.getUrl(), JiraCorePlugin.ID_PLUGIN);
		} else if (e instanceof JiraException) {
			String message = e.getMessage();
			if (message == null) {
				message = Messages.JiraCorePlugin_I_O_error_has_occured;
			}
			return new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR, ID_PLUGIN,
					RepositoryStatus.ERROR_IO, message, e);
		} else if (e instanceof ClassCastException) {
			return new RepositoryStatus(IStatus.ERROR, ID_PLUGIN, RepositoryStatus.ERROR_IO,
					Messages.JiraCorePlugin_Unexpected_server_response_ + e.getMessage(), e);
		} else if (e instanceof MalformedURLException) {
			return new RepositoryStatus(IStatus.ERROR, ID_PLUGIN, RepositoryStatus.ERROR_IO,
					Messages.JiraCorePlugin_Repository_URL_is_invalid, e);
		} else {
			return new RepositoryStatus(IStatus.ERROR, ID_PLUGIN, RepositoryStatus.ERROR_INTERNAL,
					Messages.JiraCorePlugin_Unexpected_error, e);
		}
	}

}
