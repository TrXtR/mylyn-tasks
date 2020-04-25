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

import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil;
import org.eclipse.mylyn.commons.sdk.util.ManagedSuite;
import org.eclipse.mylyn.commons.sdk.util.ManagedTestSuite;
import org.eclipse.mylyn.commons.sdk.util.TestConfiguration;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class AllJiraTests {

	public static Test suite() {
		if (CommonTestUtil.fixProxyConfiguration()) {
			CommonTestUtil.dumpSystemInfo(System.err);
		}
		TestConfiguration testConfiguration = ManagedSuite.getTestConfigurationOrCreateDefault();
		TestSuite suite = new ManagedTestSuite(AllJiraTests.class.getName());
//		addTests(suite, testConfiguration);
		addEmptyTest(suite, testConfiguration);
		return suite;
	}

	public static Test suite(TestConfiguration configuration) {
		TestSuite suite = new TestSuite(AllJiraTests.class.getName());
//		addTests(suite, configuration);
		addEmptyTest(suite, configuration);
		return suite;
	}

	private static void addEmptyTest(TestSuite suite, TestConfiguration default1) {
		suite.addTestSuite(EmptyTest.class);
	}

//	public static void addTests(TestSuite suite, TestConfiguration configuration) {
//		suite.addTest(AllJiraHeadlessStandaloneTests.suite(configuration));
//		suite.addTestSuite(JiraUtilTest.class);
//		suite.addTestSuite(JiraHyperlinkUtilTest.class);
//
//		if (!configuration.isLocalOnly()) {
//			suite.addTestSuite(JiraRepositoryQueryTest.class);
//			suite.addTestSuite(JiraRepositorySettingsPageTest.class);
//			// network tests
//			List<JiraFixture> fixtures = configuration.discover(JiraFixture.class, "jira");
//			for (JiraFixture fixture : fixtures) {
//				if (!fixture.hasTag(JiraFixture.TAG_MISC)) {
//					addTests(suite, configuration, fixture);
//				}
//			}
//		}
//	}
//
//	protected static void addTests(TestSuite suite, TestConfiguration configuration, JiraFixture fixture) {
//		fixture.createSuite(suite);
//		if (configuration.hasKind(TestKind.INTEGRATION) && !configuration.isLocalOnly()
//				&& CommonTestUtil.hasCredentials(PrivilegeLevel.ADMIN)) {
//			fixture.add(JiraTestCleanupUtil.class);
//		}
//		fixture.add(JiraRepositoryConnectorTest.class);
//		if (fixture.getAccessMode() == Version.XML_RPC) {
//			fixture.add(JiraTaskDataHandlerXmlRpcTest.class);
//			fixture.add(JiraAttachmentHandlerTest.class);
//		} else {
//			fixture.add(JiraRepositoryConnectorWebTest.class);
//		}
//		fixture.done();
//	}

}