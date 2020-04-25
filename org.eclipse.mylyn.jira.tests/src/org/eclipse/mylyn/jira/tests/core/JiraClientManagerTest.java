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

package org.eclipse.mylyn.jira.tests.core;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.jira.core.JiraClientManager;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraMilestone;
import org.eclipse.mylyn.jira.tests.support.JiraFixture;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

/**
 * @author Steffen Pingel
 */
public class JiraClientManagerTest extends TestCase {

	private TaskRepository repository;

	@Override
	protected void setUp() throws Exception {
		repository = JiraFixture.current().repository();
	}

	public void testNullCache() throws Exception {
		JiraClientManager manager = new JiraClientManager(null, new TaskRepositoryLocationFactory());
		IJiraClient client = manager.getJiraClient(repository);
		assertNull(client.getMilestones());

		manager.writeCache();
		assertNull(client.getMilestones());
	}

	public void testReadCache() throws Exception {
		File file = File.createTempFile("mylyn", null);
		file.deleteOnExit();

		JiraClientManager manager = new JiraClientManager(file, new TaskRepositoryLocationFactory());
		IJiraClient client = manager.getJiraClient(repository);
		assertNull(client.getMilestones());
	}

	public void testWriteCache() throws Exception {
		File file = File.createTempFile("mylyn", null);
		file.deleteOnExit();

		JiraClientManager manager = new JiraClientManager(file, new TaskRepositoryLocationFactory());
		IJiraClient client = manager.getJiraClient(repository);
		assertNull(client.getMilestones());

		client.updateAttributes(new NullProgressMonitor(), false);
		assertTrue(client.getMilestones().length > 0);
		JiraMilestone[] milestones = client.getMilestones();

		manager.writeCache();
		manager = new JiraClientManager(file, new TaskRepositoryLocationFactory());
		assertEquals(Arrays.asList(milestones), Arrays.asList(client.getMilestones()));
	}

}
