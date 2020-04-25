/*******************************************************************************
 * Copyright (c) 2006, 2009 Steffen Pingel and others.
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

package org.eclipse.mylyn.jira.tests.client;

import org.eclipse.mylyn.commons.net.WebLocation;
import org.eclipse.mylyn.commons.repositories.core.auth.UserCredentials;
import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil;
import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil.PrivilegeLevel;
import org.eclipse.mylyn.internal.jira.core.JiraClientFactory;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient.Version;
import org.eclipse.mylyn.internal.jira.core.client.JiraException;
import org.eclipse.mylyn.internal.jira.core.client.JiraLoginException;
import org.eclipse.mylyn.internal.jira.core.client.JiraWebClient;
import org.eclipse.mylyn.internal.jira.core.client.JiraXmlRpcClient;
import org.eclipse.mylyn.jira.tests.support.JiraFixture;

import junit.framework.TestCase;

/**
 * @author Steffen Pingel
 */
public class JiraClientFactoryTest extends TestCase {

	private JiraFixture fixture;

	@Override
	protected void setUp() throws Exception {
		fixture = JiraFixture.current();
	}

	public void testCreateClient() throws Exception {
		WebLocation location = new WebLocation(fixture.getRepositoryUrl(), "user", "password");
		IJiraClient client = JiraClientFactory.createClient(location, fixture.getAccessMode());
		if (fixture.getAccessMode() == Version.JIRA_0_9) {
			assertTrue(client instanceof JiraWebClient);
		} else {
			assertTrue(client instanceof JiraXmlRpcClient);
		}
	}

	public void testCreateClientNull() throws Exception {
		WebLocation location = new WebLocation(fixture.getRepositoryUrl(), "user", "password");
		IJiraClient client = JiraClientFactory.createClient(location, null);
		assertEquals(Version.XML_RPC, client.getAccessMode());
	}

	public void testProbeClient() throws Exception {
		String url = fixture.getRepositoryUrl();

		UserCredentials credentials = CommonTestUtil.getCredentials(PrivilegeLevel.USER);
		WebLocation location = new WebLocation(url, credentials.getUserName(), credentials.getPassword());
		Version version = JiraClientFactory.probeClient(location);
		if (fixture.isXmlRpcEnabled()) {
			// assertion is only meaningful for XML-RPC since web fixtures will also probe XML-RPC if available
			assertEquals(Version.XML_RPC, version);
		}
	}

	public void testProbeClientNoCredentials() throws Exception {
		String url = fixture.getRepositoryUrl();
		WebLocation location = new WebLocation(url, "", "");
		try {
			Version version = JiraClientFactory.probeClient(location);
			if (fixture.requiresAuthentication()) {
				fail("Expected JiraLoginException");
			}
			assertEquals(Version.JIRA_0_9, version);
		} catch (JiraLoginException e) {
			if (fixture.requiresAuthentication()) {
				// the remainder of the
				return;
			}
			throw e;
		}
	}

	public void testProbeClientInvalidCredentials() throws Exception {
		try {
			WebLocation location = new WebLocation(fixture.getRepositoryUrl(), "invaliduser", "password");
			Version version = JiraClientFactory.probeClient(location);
			fail("Expected JiraLoginException, got " + version);
		} catch (JiraLoginException e) {
		}
	}

	public void testProbeClientInvalidLocation() throws Exception {
		try {
			WebLocation location = new WebLocation(fixture.getRepositoryUrl() + "/nonexistant", "", "");
			Version version = JiraClientFactory.probeClient(location);
			fail("Expected JiraException, got " + version);
		} catch (JiraException e) {
		}
	}

}
