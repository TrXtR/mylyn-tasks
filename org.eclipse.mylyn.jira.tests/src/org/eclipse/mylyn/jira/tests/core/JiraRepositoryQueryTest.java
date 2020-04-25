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

package org.eclipse.mylyn.jira.tests.core;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.ui.RefactorRepositoryUrlOperation;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * @author Steffen Pingel
 */
public class JiraRepositoryQueryTest extends TestCase {

	public void testChangeRepositoryUrl() throws Exception {
		TaskRepository repository = new TaskRepository(JiraCorePlugin.CONNECTOR_KIND, "http://mylyn.org/jira-one");

		JiraSearch search = new JiraSearch();
		String queryUrl = repository.getRepositoryUrl() + IJiraClient.QUERY_URL + search.toUrl();
		IRepositoryQuery query = TasksUi.getRepositoryModel().createRepositoryQuery(repository);
		query.setUrl(queryUrl);
		TasksUiPlugin.getTaskList().addQuery((RepositoryQuery) query);

		String taskId = "123";
		ITask task = TasksUi.getRepositoryModel().createTask(repository, taskId);
		task.setUrl(repository.getRepositoryUrl() + IJiraClient.TICKET_URL + taskId);
		TasksUiPlugin.getTaskList().addTask(task);

		String oldUrl = repository.getRepositoryUrl();
		String newUrl = "http://mylyn.org/jira-two";
		new RefactorRepositoryUrlOperation(oldUrl, newUrl).run(new NullProgressMonitor());
		repository.setRepositoryUrl(newUrl);

		assertEquals(newUrl, query.getRepositoryUrl());
		assertEquals(newUrl + IJiraClient.QUERY_URL + search.toUrl(), query.getUrl());
		assertEquals(newUrl + IJiraClient.TICKET_URL + taskId, task.getUrl());
	}

}
