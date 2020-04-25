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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.repositories.core.auth.UserCredentials;
import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil;
import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil.PrivilegeLevel;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient.Version;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearchFilter.CompareOperator;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket.Key;
import org.eclipse.mylyn.jira.tests.support.JiraFixture;
import org.eclipse.mylyn.jira.tests.support.JiraHarness;
import org.eclipse.mylyn.jira.tests.support.JiraTestUtil;

import junit.framework.TestCase;

/**
 * Test cases for classes that implement {@link IJiraClient}.
 *
 * @author Steffen Pingel
 */
public class JiraClientTest extends TestCase {

	private IJiraClient client;

	private JiraFixture fixture;

	private JiraHarness harness;

	public JiraClientTest() {
	}

	@Override
	protected void setUp() throws Exception {
		fixture = JiraFixture.current();
		harness = fixture.createHarness();
		client = fixture.connect();
	}

	@Override
	protected void tearDown() throws Exception {
		harness.dispose();
	}

	public void testGetTicket() throws Exception {
		JiraTicket expectedTicket = harness.createTicket("getTicket");
		JiraTicket ticket = client.getTicket(expectedTicket.getId(), null);
		JiraTestUtil.assertTicketEquals(client.getAccessMode(), expectedTicket, ticket);
	}

	public void testGetTicketInvalidId() throws Exception {
		try {
			client.getTicket(Integer.MAX_VALUE, null);
			fail("Expected JiraException");
		} catch (JiraException e) {
		}
	}

	public void testGetTicketUmlaute() throws Exception {
		JiraTicket ticket = harness.newTicket("test html entities: \u00E4\u00F6\u00FC");
		ticket.putBuiltinValue(Key.DESCRIPTION, "\u00C4\u00D6\u00DC\n\nmulti\nline\n\n'''bold'''\n");
		ticket = harness.createTicket(ticket);

		ticket = client.getTicket(ticket.getId(), null);
		assertEquals("test html entities: \u00E4\u00F6\u00FC", ticket.getValue(Key.SUMMARY));
		if (client.getAccessMode() == Version.XML_RPC) {
			assertEquals("\u00C4\u00D6\u00DC\n\nmulti\nline\n\n'''bold'''\n", ticket.getValue(Key.DESCRIPTION));
		} else {
			assertEquals(null, ticket.getValue(Key.DESCRIPTION));
		}
	}

	public void testProxy() throws Exception {
		client = fixture.connect(fixture.getRepositoryUrl(), "", "",
				new Proxy(Type.HTTP, new InetSocketAddress("invalidhostname", 8080)));
		try {
			client.validate(new NullProgressMonitor());
			fail("Expected IOException");
		} catch (JiraException e) {
		}
	}

	public void testSearchAll() throws Exception {
		harness.createTicket("searchAllTickets");
		JiraSearch search = new JiraSearch();
		List<JiraTicket> result = new ArrayList<JiraTicket>();
		client.search(search, result, null);
		assertTrue(!result.isEmpty());
	}

	public void testSearchEmpty() throws Exception {
		JiraSearch search = new JiraSearch();
		search.addFilter("milestone", "does not exist");
		List<JiraTicket> result = new ArrayList<JiraTicket>();
		client.search(search, result, null);
		assertEquals(0, result.size());
	}

	public void testSearchExactMatch() throws Exception {
		String uniqueTag = RandomStringUtils.randomAlphanumeric(6);
		String summary = "searchExactMatch " + uniqueTag;
		JiraTicket ticket = harness.createTicketWithMilestone(summary, "milestone1");

		JiraSearch search = new JiraSearch();
		search.addFilter("milestone", "milestone1");
		search.addFilter("summary", summary);
		List<JiraTicket> result = new ArrayList<JiraTicket>();
		client.search(search, result, null);
		assertEquals(1, result.size());
		JiraTestUtil.assertTicketEquals(ticket, result.get(0));
		assertEquals("milestone1", result.get(0).getValue(Key.MILESTONE));
		assertEquals(summary, result.get(0).getValue(Key.SUMMARY));
	}

	public void testSearchMilestone1() throws Exception {
		String uniqueTag = RandomStringUtils.randomAlphanumeric(6);
		JiraTicket ticket = harness.createTicketWithMilestone("searchMilestone1" + uniqueTag, "milestone1");
		harness.createTicketWithMilestone("searchMilestone1" + uniqueTag, "milestone2");

		JiraSearch search = new JiraSearch();
		search.addFilter(new JiraSearchFilter("summary", CompareOperator.CONTAINS, uniqueTag));
		search.addFilter("milestone", "milestone1");
		search.addFilter("milestone", "milestone1");
		List<JiraTicket> result = new ArrayList<JiraTicket>();
		client.search(search, result, null);
		assertEquals(1, result.size());
		JiraTestUtil.assertTicketEquals(ticket, result.get(0));
	}

	public void testSearchMilestone2() throws Exception {
		String uniqueTag = RandomStringUtils.randomAlphanumeric(6);
		JiraTicket ticket1 = harness.createTicketWithMilestone("searchMilestone2 " + uniqueTag, "milestone1");
		JiraTicket ticket2 = harness.createTicketWithMilestone("searchMilestone2 " + uniqueTag, "milestone1");
		JiraTicket ticket3 = harness.createTicketWithMilestone("searchMilestone2 " + uniqueTag, "milestone2");

		JiraSearch search = new JiraSearch();
		search.addFilter(new JiraSearchFilter("summary", CompareOperator.CONTAINS, uniqueTag));
		search.addFilter("milestone", "milestone1");
		search.addFilter("milestone", "milestone2");
		search.setOrderBy("id");
		List<JiraTicket> result = new ArrayList<JiraTicket>();
		client.search(search, result, null);
		assertEquals(3, result.size());
		JiraTestUtil.assertTicketEquals(ticket1, result.get(0));
		JiraTestUtil.assertTicketEquals(ticket2, result.get(1));
		JiraTestUtil.assertTicketEquals(ticket3, result.get(2));
	}

	public void testSearchMilestoneAmpersand() throws Exception {
		if (!harness.hasMilestone("mile&stone")) {
			// ignore test
			return;
		}

		JiraTicket ticket = harness.createTicketWithMilestone("searchMilestoneAmpersand", "mile&stone");

		JiraSearch search = new JiraSearch();
		search.addFilter("milestone", "mile&stone");
		search.setOrderBy("id");
		List<JiraTicket> result = new ArrayList<JiraTicket>();
		try {
			client.search(search, result, null);
			assertEquals(1, result.size());
			JiraTestUtil.assertTicketEquals(ticket, result.get(0));
		} catch (JiraRemoteException e) {
			if ("'Query filter requires field and constraints separated by a \"=\"' while executing 'ticket.query()'"
					.equals(e.getMessage())
					&& (fixture.getVersion().equals("0.10") || fixture.getVersion().equals("0.11"))) {
				// ignore upstream problem, see bug 162094
			} else {
				throw e;
			}
		}
	}

	public void testStatusClosed() throws Exception {
		JiraTicket ticket = harness.createTicket("statusClosed");
		ticket.putBuiltinValue(Key.STATUS, "closed");
		ticket.putBuiltinValue(Key.RESOLUTION, "fixed");
		harness.udpateTicket(ticket);

		ticket = client.getTicket(ticket.getId(), null);
		assertEquals("closed", ticket.getValue(Key.STATUS));
		assertEquals("fixed", ticket.getValue(Key.RESOLUTION));
	}

	public void testUpdateAttributesAnonymous() throws Exception {
		if (fixture.requiresAuthentication()) {
			return;
		}

		client = fixture.connect(fixture.getRepositoryUrl(), "", "");
		assertNull(client.getMilestones());
		try {
			client.updateAttributes(new NullProgressMonitor(), true);
			if (fixture.getAccessMode() == Version.XML_RPC) {
				fail("Expected anonymous access to be denied");
			}
		} catch (JiraPermissionDeniedException e) {
			if (fixture.getAccessMode() == Version.XML_RPC) {
				return; // expected exception, done here
			}
			throw e;
		}
		JiraVersion[] versions = client.getVersions();
		assertEquals(2, versions.length);
		Arrays.sort(versions, new Comparator<JiraVersion>() {
			public int compare(JiraVersion o1, JiraVersion o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		assertEquals("1.0", versions[0].getName());
		assertEquals("2.0", versions[1].getName());
	}

	public void testUpdateAttributesChangedTicketFields() throws Exception {
		if (fixture.getAccessMode() == Version.JIRA_0_9) {
			// field information is not available in web mode
			return;
		}

		client = fixture.connect(fixture.getRepositoryUrl());
		client.updateAttributes(new NullProgressMonitor(), true);
		// modify field to bogus value
		JiraTicketField field = client.getTicketFieldByName(JiraAttribute.MILESTONE.getJiraKey());
		field.setDefaultValue("modified default value");

		// updating should reset modified field
		client.updateAttributes(new NullProgressMonitor(), true);
		field = client.getTicketFieldByName(JiraAttribute.MILESTONE.getJiraKey());
		assertEquals("", field.getDefaultValue());
	}

	public void testValidate() throws Exception {
		UserCredentials credentials = CommonTestUtil.getCredentials(PrivilegeLevel.USER);

		// standard connect
		client.validate(new NullProgressMonitor());

		// invalid url
		client = JiraFixture.current().connect("http://non.existant/repository");
		try {
			client.validate(new NullProgressMonitor());
			fail("Expected JiraException");
		} catch (JiraException e) {
		}

		String url = JiraFixture.current().getRepositoryUrl();

		// invalid password
		client = JiraFixture.current().connect(url, credentials.getUserName(), "wrongpassword");
		try {
			client.validate(new NullProgressMonitor());
			fail("Expected JiraLoginException");
		} catch (JiraLoginException e) {
		}

		// invalid username
		client = JiraFixture.current().connect(url, "wrongusername", credentials.getPassword());
		try {
			client.validate(new NullProgressMonitor());
			fail("Expected JiraLoginException");
		} catch (JiraLoginException e) {
		}
	}

	public void testValidateAnonymousLogin() throws Exception {
		if (fixture.requiresAuthentication()) {
			return;
		}

		client = fixture.connect(fixture.getRepositoryUrl(), "", "");
		try {
			client.validate(new NullProgressMonitor());
			if (fixture.getAccessMode() == Version.XML_RPC) {
				fail("Expected anonymous access to be denied");
			}
		} catch (JiraPermissionDeniedException e) {
			if (fixture.getAccessMode() == Version.JIRA_0_9) {
				fail("Expected anonymous access to be allowed");
			}
		}
	}

	public void testValidateAnyPage() throws Exception {
		client = fixture.connect("http://mylyn.eclipse.org/");
		try {
			client.validate(new NullProgressMonitor());
			fail("Expected JiraException");
		} catch (JiraException e) {
		}
	}

}
