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
 *     Benjamin Muskalla (Tasktop Technologies) - support for deleting tasks     
 *******************************************************************************/

package org.eclipse.mylyn.jira.tests.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.tasks.core.TaskTask;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.util.AttachmentUtil;
import org.eclipse.mylyn.internal.jira.core.JiraRepositoryConnector;
import org.eclipse.mylyn.internal.jira.core.JiraTaskDataHandler;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient.Version;
import org.eclipse.mylyn.internal.jira.core.model.JiraPriority;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket.Key;
import org.eclipse.mylyn.jira.tests.support.JiraFixture;
import org.eclipse.mylyn.jira.tests.support.JiraHarness;
import org.eclipse.mylyn.jira.tests.support.JiraTestUtil;
import org.eclipse.mylyn.internal.jira.core.model.JiraVersion;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.PlatformUI;

/**
 * @author Steffen Pingel
 */
public class JiraRepositoryConnectorTest extends TestCase {

	private TaskRepository repository;

	private JiraRepositoryConnector connector;

	private JiraHarness harness;

	@Override
	protected void setUp() throws Exception {
		harness = JiraFixture.current().createHarness();
		connector = harness.connector();
		repository = harness.repository();
	}

	@Override
	protected void tearDown() throws Exception {
		harness.dispose();
	}

	public void testGetRepositoryUrlFromTaskUrl() {
		assertEquals("http://host/repo", connector.getRepositoryUrlFromTaskUrl("http://host/repo/ticket/1"));
		assertEquals("http://host", connector.getRepositoryUrlFromTaskUrl("http://host/ticket/2342"));
		assertEquals(null, connector.getRepositoryUrlFromTaskUrl("http://host/repo/2342"));
		assertEquals(null, connector.getRepositoryUrlFromTaskUrl("http://host/repo/ticket-2342"));
	}

	public void testGetTaskData() throws Exception {
		JiraTicket ticket = harness.createTicket("createTaskFromExistingKeyXml");
		String taskId = Integer.toString(ticket.getId());
		TaskData taskData = connector.getTaskData(repository, taskId, null);
		ITask task = TasksUi.getRepositoryModel().createTask(repository, taskData.getTaskId());
		assertNotNull(task);
		connector.updateTaskFromTaskData(repository, task, taskData);
		assertEquals(TaskTask.class, task.getClass());
		assertEquals("createTaskFromExistingKeyXml", task.getSummary());
		assertEquals(repository.getRepositoryUrl() + IJiraClient.TICKET_URL + taskId, task.getUrl());
	}

	public void testPerformQuery() {
		JiraSearch search = new JiraSearch();
		search.addFilter("milestone", "milestone1");
		search.addFilter("milestone", "milestone2");
		search.setOrderBy("id");
		IRepositoryQuery query = TasksUi.getRepositoryModel().createRepositoryQuery(repository);
		query.setUrl(repository.getUrl() + IJiraClient.QUERY_URL + search.toUrl());

		final List<TaskData> result = new ArrayList<TaskData>();
		TaskDataCollector hitCollector = new TaskDataCollector() {
			@Override
			public void accept(TaskData hit) {
				result.add(hit);
			}
		};
		IStatus queryStatus = connector.performQuery(repository, query, hitCollector, null, new NullProgressMonitor());
		assertEquals(Status.OK_STATUS, queryStatus);
//		assertEquals(3, result.size());
//		assertEquals(data.tickets.get(0).getId() + "", result.get(0).getTaskId());
//		assertEquals(data.tickets.get(1).getId() + "", result.get(1).getTaskId());
//		assertEquals(data.tickets.get(2).getId() + "", result.get(2).getTaskId());
	}

	public void testUpdateAttributes() throws Exception {
		connector.updateRepositoryConfiguration(repository, new NullProgressMonitor());

		IJiraClient server = connector.getClientManager().getJiraClient(repository);
		JiraVersion[] versions = server.getVersions();
		assertEquals(2, versions.length);
		Arrays.sort(versions, new Comparator<JiraVersion>() {
			public int compare(JiraVersion o1, JiraVersion o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		assertEquals("1.0", versions[0].getName());
		assertEquals("2.0", versions[1].getName());
	}

	public void testContext() throws Exception {
		ITask task = harness.createTask("context");
		File sourceContextFile = TasksUiPlugin.getContextStore().getFileForContext(task);
		sourceContextFile.createNewFile();
		sourceContextFile.deleteOnExit();

		boolean result;
		try {
			result = AttachmentUtil.postContext(connector, repository, task, "", null, null);
			if (repository.getVersion().equals(Version.JIRA_0_9.name())) {
				fail("expected CoreException"); // operation should not be supported
			}
		} catch (CoreException e) {
			if (repository.getVersion().equals(Version.JIRA_0_9.name())) {
				// done
				return;
			}
			throw e;
		}

		assertTrue(result);
		task = harness.getTask(task.getTaskId());
		List<ITaskAttachment> attachments = JiraTestUtil.getTaskAttachments(task);
		// TODO attachment may have been overridden therefore size may not have changed
		//assertEquals(size + 1, task.getTaskData().getAttachments().size());
		ITaskAttachment attachment = attachments.get(attachments.size() - 1);
		result = AttachmentUtil.downloadContext(task, attachment, PlatformUI.getWorkbench().getProgressService());
		assertTrue(result);
		assertTrue(task.isActive());
	}

	public void testIsCompleted() {
		assertTrue(JiraRepositoryConnector.isCompleted("closed"));
		assertFalse(JiraRepositoryConnector.isCompleted("Closed"));
		assertFalse(JiraRepositoryConnector.isCompleted("new"));
		assertFalse(JiraRepositoryConnector.isCompleted("assigned"));
		assertFalse(JiraRepositoryConnector.isCompleted("reopened"));
		assertFalse(JiraRepositoryConnector.isCompleted("foobar"));
		assertFalse(JiraRepositoryConnector.isCompleted(""));
		assertFalse(JiraRepositoryConnector.isCompleted(null));
	}

	public void testGetTaskPriority() {
		assertEquals("P1", JiraRepositoryConnector.getTaskPriority("blocker").toString());
		assertEquals("P2", JiraRepositoryConnector.getTaskPriority("critical").toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority("major").toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority(null).toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority("").toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority("foo bar").toString());
		assertEquals("P4", JiraRepositoryConnector.getTaskPriority("minor").toString());
		assertEquals("P5", JiraRepositoryConnector.getTaskPriority("trivial").toString());
	}

	public void testGetTaskPriorityFromJiraPriorities() {
		JiraPriority p1 = new JiraPriority("a", 1);
		JiraPriority p2 = new JiraPriority("b", 2);
		JiraPriority p3 = new JiraPriority("c", 3);
		JiraPriority[] priorities = new JiraPriority[] { p1, p2, p3 };
		assertEquals("P1", JiraRepositoryConnector.getTaskPriority("a", priorities).toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority("b", priorities).toString());
		assertEquals("P5", JiraRepositoryConnector.getTaskPriority("c", priorities).toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority("foo", priorities).toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority(null, priorities).toString());

		p1 = new JiraPriority("a", 10);
		priorities = new JiraPriority[] { p1 };
		assertEquals("P1", JiraRepositoryConnector.getTaskPriority("a", priorities).toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority("b", priorities).toString());
		assertEquals("P3", JiraRepositoryConnector.getTaskPriority(null, priorities).toString());

		p1 = new JiraPriority("1", 10);
		p2 = new JiraPriority("2", 20);
		p3 = new JiraPriority("3", 30);
		JiraPriority p4 = new JiraPriority("4", 40);
		JiraPriority p5 = new JiraPriority("5", 70);
		JiraPriority p6 = new JiraPriority("6", 100);
		priorities = new JiraPriority[] { p1, p2, p3, p4, p5, p6 };
		assertEquals("P1", JiraRepositoryConnector.getTaskPriority("1", priorities).toString());
		assertEquals("P1", JiraRepositoryConnector.getTaskPriority("2", priorities).toString());
		assertEquals("P2", JiraRepositoryConnector.getTaskPriority("3", priorities).toString());
		assertEquals("P2", JiraRepositoryConnector.getTaskPriority("4", priorities).toString());
		assertEquals("P4", JiraRepositoryConnector.getTaskPriority("5", priorities).toString());
		assertEquals("P5", JiraRepositoryConnector.getTaskPriority("6", priorities).toString());
	}

	public void testUpdateTaskFromTaskData() throws Exception {
		JiraTicket ticket = new JiraTicket(123);
		ticket.putBuiltinValue(Key.DESCRIPTION, "mydescription");
		ticket.putBuiltinValue(Key.PRIORITY, "mypriority");
		ticket.putBuiltinValue(Key.SUMMARY, "mysummary");
		ticket.putBuiltinValue(Key.TYPE, "mytype");

		JiraTaskDataHandler taskDataHandler = connector.getTaskDataHandler();
		IJiraClient client = connector.getClientManager().getJiraClient(repository);
		client.updateAttributes(new NullProgressMonitor(), false);
		TaskData taskData = taskDataHandler.createTaskDataFromTicket(client, repository, ticket, null);
		ITask task = TasksUi.getRepositoryModel().createTask(repository, taskData.getTaskId());

		connector.updateTaskFromTaskData(repository, task, taskData);
		assertEquals(repository.getRepositoryUrl() + IJiraClient.TICKET_URL + "123", task.getUrl());
		assertEquals("123", task.getTaskKey());
		assertEquals("mysummary", task.getSummary());
		assertEquals("P3", task.getPriority());
		assertEquals("mytype", task.getTaskKind());
	}

	public void testUpdateTaskFromTaskDataSummaryOnly() throws Exception {
		JiraTaskDataHandler taskDataHandler = connector.getTaskDataHandler();
		IJiraClient client = connector.getClientManager().getJiraClient(repository);
		// ensure that client has the correct field configuration
		client.updateAttributes(new NullProgressMonitor(), true);
		assertEquals(client.getAccessMode().name(), repository.getVersion());

		// prepare task data
		JiraTicket ticket = new JiraTicket(456);
		ticket.putBuiltinValue(Key.SUMMARY, "mysummary");
		TaskData taskData = taskDataHandler.createTaskDataFromTicket(client, repository, ticket, null);
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(TaskAttribute.PRIORITY);
		if (attribute != null) {
			assertEquals("major", attribute.getValue());
		}

		ITask task = TasksUi.getRepositoryModel().createTask(repository, taskData.getTaskId());
		task.setPriority("P2");

		// create task from task data
		connector.updateTaskFromTaskData(repository, task, taskData);
		assertEquals(repository.getRepositoryUrl() + IJiraClient.TICKET_URL + "456", task.getUrl());
		assertEquals("456", task.getTaskKey());
		assertEquals("mysummary", task.getSummary());
		// depending on the access mode createTaskDataFromTicket() creates different default attributes  
		if (client.getAccessMode() == Version.JIRA_0_9) {
			// the ticket type varies depending on Jira version
			//assertEquals(AbstractTask.DEFAULT_TASK_KIND, task.getTaskKind());
			assertEquals("P2", task.getPriority());
		} else {
			assertEquals("Defect", task.getTaskKind());
			assertEquals("P3", task.getPriority());
		}
	}

	public void testUpdateTaskFromTaskDataClosed() throws Exception {
		JiraTaskDataHandler taskDataHandler = connector.getTaskDataHandler();
		IJiraClient client = connector.getClientManager().getJiraClient(repository);
		ITask task = TasksUi.getRepositoryModel().createTask(repository, "1");

		JiraTicket ticket = new JiraTicket(123);
		ticket.putBuiltinValue(Key.STATUS, "resolved");
		TaskData taskData = taskDataHandler.createTaskDataFromTicket(client, repository, ticket, null);
		connector.updateTaskFromTaskData(repository, task, taskData);
		assertEquals(null, task.getCompletionDate());

		ticket.putBuiltinValue(Key.STATUS, "closed");
		taskData = taskDataHandler.createTaskDataFromTicket(client, repository, ticket, null);
		connector.updateTaskFromTaskData(repository, task, taskData);
		assertEquals(new Date(0), task.getCompletionDate());

		ticket.putBuiltinValue(Key.STATUS, "closed");
		ticket.putBuiltinValue(Key.CHANGE_TIME, "123");
		taskData = taskDataHandler.createTaskDataFromTicket(client, repository, ticket, null);
		connector.updateTaskFromTaskData(repository, task, taskData);
		assertEquals(new Date(123 * 1000), task.getCompletionDate());
	}

	public void testDeleteNewTask() throws Exception {
		IJiraClient client = connector.getClientManager().getJiraClient(repository);
		if (client.getAccessMode() == Version.JIRA_0_9) {
			// not supported in web mode
			return;
		}
		ITask task = harness.createTask("deleteNewTask");
		assertTrue(connector.canDeleteTask(repository, task));
		connector.deleteTask(repository, task, null);
		try {
			connector.getTaskData(repository, task.getTaskId(), null);
			fail("Task should be gone");
		} catch (CoreException e) {
			assertTrue(e.getMessage().contains("does not exist"));
		}
	}

}
