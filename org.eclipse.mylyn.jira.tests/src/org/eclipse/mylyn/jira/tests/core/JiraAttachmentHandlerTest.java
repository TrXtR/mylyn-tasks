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

package org.eclipse.mylyn.jira.tests.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.mylyn.internal.jira.core.JiraRepositoryConnector;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket;
import org.eclipse.mylyn.internal.tasks.core.data.FileTaskAttachmentSource;
import org.eclipse.mylyn.jira.tests.support.JiraFixture;
import org.eclipse.mylyn.jira.tests.support.JiraHarness;
import org.eclipse.mylyn.jira.tests.support.JiraTestUtil;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;

import junit.framework.TestCase;

/**
 * @author Steffen Pingel
 */
public class JiraAttachmentHandlerTest extends TestCase {

	private TaskRepository repository;

	private JiraRepositoryConnector connector;

	private AbstractTaskAttachmentHandler attachmentHandler;

	private JiraHarness harness;

	@Override
	protected void setUp() throws Exception {
		harness = JiraFixture.current().createHarness();
		connector = harness.connector();
		attachmentHandler = connector.getTaskAttachmentHandler();
		repository = harness.repository();
	}

	@Override
	protected void tearDown() throws Exception {
		harness.dispose();
	}

	public void testGetContent() throws Exception {
		JiraTicket ticket = harness.createTicket("GetContent");
		harness.attachFile(ticket.getId(), "attachment.txt", "Mylar\n");

		ITask task = harness.getTask(ticket);
		List<ITaskAttachment> attachments = JiraTestUtil.getTaskAttachments(task);
		assertTrue(attachments.size() > 0);
		InputStream in = attachmentHandler.getContent(repository, task, attachments.get(0).getTaskAttribute(), null);
		try {
			byte[] result = new byte[6];
			in.read(result);
			assertEquals("Mylar\n", new String(result));
			assertEquals(-1, in.read());
		} finally {
			in.close();
		}
	}

	public void testPostConent() throws Exception {
		ITask task = harness.createTask("GetContent");
		File file = File.createTempFile("attachment", null);
		file.deleteOnExit();
		try (OutputStream out = new FileOutputStream(file)) {
			out.write("Mylar".getBytes());
		}
		attachmentHandler.postContent(repository, task, new FileTaskAttachmentSource(file), "comment", null, null);

		IJiraClient client = connector.getClientManager().getJiraClient(repository);
		try (InputStream in = client.getAttachmentData(Integer.parseInt(task.getTaskId()), file.getName(), null)) {
			byte[] result = new byte[5];
			in.read(result);
			assertEquals("Mylar", new String(result));
		}
	}

	public void testCanUploadAttachment() throws Exception {
		ITask task = harness.createTask("canUploadAttachment");
		if (harness.isXmlRpc()) {
			assertTrue(attachmentHandler.canPostContent(repository, task));
		} else {
			assertFalse(attachmentHandler.canPostContent(repository, task));
		}
	}

	public void testCanDownloadAttachmentXmlRpc() throws Exception {
		ITask task = harness.createTask("canDownloadAttachment");
		if (harness.isXmlRpc()) {
			assertTrue(attachmentHandler.canGetContent(repository, task));
		} else {
			assertFalse(attachmentHandler.canGetContent(repository, task));
		}
	}

}
