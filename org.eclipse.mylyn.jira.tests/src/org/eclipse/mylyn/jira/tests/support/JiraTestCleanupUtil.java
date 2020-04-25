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

package org.eclipse.mylyn.jira.tests.support;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil.PrivilegeLevel;
import org.eclipse.mylyn.commons.sdk.util.TestConfiguration;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.JiraException;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import junit.framework.TestCase;

/**
 * Utility that cleans up artifacts created by the Jira test suite. This class should be run periodically to speed up
 * execution of (attachment) tests.
 *
 * @author Steffen Pingel
 */
public class JiraTestCleanupUtil extends TestCase {

	@RunWith(Parameterized.class)
	public static class JiraTestCleanupUtil4 extends JiraTestCleanupUtil {

		public JiraTestCleanupUtil4(JiraFixture fixture, String name) {
			super(name);
			setFixture(fixture);
		}

		// requires JUnit 4.11 @Parameters(name = "{1}")
		@Parameters
		public static Iterable<Object[]> data() {
			List<JiraFixture> fixtures = TestConfiguration.getDefault().discover(JiraFixture.class, "jira");
			List<Object[]> data = new ArrayList<Object[]>(fixtures.size());
			for (JiraFixture fixture : fixtures) {
				data.add(new Object[] { fixture, fixture.getInfo() });
			}
			return data;
		}

	}

	private JiraFixture fixture;

	public JiraTestCleanupUtil(String name) {
		super(name);
		this.fixture = JiraFixture.current();
	}

	protected void setFixture(JiraFixture fixture) {
		this.fixture = fixture;
	}

	@Test
	public void testCleanUpTasks() throws Exception {
		System.err.println("Connected to " + fixture.getRepositoryUrl());
		IJiraClient client = fixture.connectXmlRpc(PrivilegeLevel.ADMIN);
		deleteOldTickets(client);
	}

	public void deleteOldTickets(IJiraClient client) throws JiraException {
		JiraSearch query = new JiraSearch();
		query.setMax(10000);
		List<Integer> result = new ArrayList<Integer>();
		client.searchForTicketIds(query, result, null);
		System.err.println("Found " + result.size() + " tickets");
		System.err.print("Deleting ticket: ");
		for (Integer i : result) {
			if (i > 10) {
				System.err.print(i + ", ");
				client.deleteTicket(i, null);
				if (i % 20 == 0) {
					System.err.println();
					System.err.print(" ");
				}
			}
		}
		System.err.println();
	}

}
