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

package org.eclipse.mylyn.jira.tests;

import java.util.List;

import org.eclipse.mylyn.commons.sdk.util.TestConfiguration;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient.Version;
import org.eclipse.mylyn.jira.tests.client.JiraClientFactoryTest;
import org.eclipse.mylyn.jira.tests.client.JiraClientProxyTest;
import org.eclipse.mylyn.jira.tests.client.JiraClientTest;
import org.eclipse.mylyn.jira.tests.client.JiraRepositoryInfoTest;
import org.eclipse.mylyn.jira.tests.client.JiraSearchTest;
import org.eclipse.mylyn.jira.tests.client.JiraTicketTest;
import org.eclipse.mylyn.jira.tests.client.JiraXmlRpcClientTest;
import org.eclipse.mylyn.jira.tests.core.JiraClientManagerTest;
import org.eclipse.mylyn.jira.tests.support.JiraFixture;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Steffen Pingel
 */
public class AllJiraHeadlessStandaloneTests {

	public static Test suite() {
		return suite(TestConfiguration.getDefault());
	}

	public static Test suite(TestConfiguration configuration) {
		TestSuite suite = new TestSuite(AllJiraHeadlessStandaloneTests.class.getName());
		// client tests
		suite.addTestSuite(JiraSearchTest.class);
		suite.addTestSuite(JiraTicketTest.class);
		suite.addTestSuite(JiraRepositoryInfoTest.class);
		suite.addTestSuite(JiraClientProxyTest.class);
		// network tests
		if (!configuration.isLocalOnly()) {
			List<JiraFixture> fixtures = configuration.discover(JiraFixture.class, "jira");
			for (JiraFixture fixture : fixtures) {
				if (fixture.hasTag(JiraFixture.TAG_MISC)) {
					fixture.createSuite(suite);
					fixture.add(JiraClientFactoryTest.class);
					fixture.add(JiraClientTest.class);
					fixture.done();
				} else if (!fixture.hasTag(JiraFixture.TAG_TEST)) {
					addTests(suite, fixture);
				}
			}
		}
		return suite;
	}

	private static void addTests(TestSuite suite, JiraFixture fixture) {
		fixture.createSuite(suite);
		fixture.add(JiraClientManagerTest.class);
		fixture.add(JiraClientFactoryTest.class);
		fixture.add(JiraClientTest.class);
		if (fixture.getAccessMode() == Version.XML_RPC) {
			fixture.add(JiraXmlRpcClientTest.class);
		}
		fixture.done();
	}

}