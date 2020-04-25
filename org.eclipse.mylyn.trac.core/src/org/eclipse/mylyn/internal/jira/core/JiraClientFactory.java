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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.JiraException;
import org.eclipse.mylyn.internal.jira.core.client.JiraLoginException;
import org.eclipse.mylyn.internal.jira.core.client.JiraWebClient;
import org.eclipse.mylyn.internal.jira.core.client.JiraXmlRpcClient;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient.Version;

/**
 * @author Steffen Pingel
 */
public class JiraClientFactory {

	public static IJiraClient createClient(AbstractWebLocation location, Version version) {
		if (version == Version.JIRA_0_9) {
			return new JiraWebClient(location, version);
		} else if (version == Version.XML_RPC) {
			return new JiraXmlRpcClient(location, version);
		}

		// fall-back to XML_RPC in case the repository information is incomplete
		return new JiraXmlRpcClient(location, Version.XML_RPC);
	}

	/**
	 * Tries all supported access types for <code>location</code> and returns the corresponding version if successful;
	 * throws an exception otherwise.
	 * <p>
	 * Order of the tried access types: XML-RPC, Jira 0.9
	 */
	public static Version probeClient(AbstractWebLocation location) throws MalformedURLException, JiraException {
		try {
			IJiraClient repository = new JiraXmlRpcClient(location, Version.XML_RPC);
			repository.validate(new NullProgressMonitor());
			return Version.XML_RPC;
		} catch (JiraException e) {
			try {
				IJiraClient repository = new JiraWebClient(location, Version.JIRA_0_9);
				repository.validate(new NullProgressMonitor());
				return Version.JIRA_0_9;
			} catch (JiraLoginException e2) {
				throw e;
			} catch (JiraException e2) {
			}
		}

		throw new JiraException();
	}

}
