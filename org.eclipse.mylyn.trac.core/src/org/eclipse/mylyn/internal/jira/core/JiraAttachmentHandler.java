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

package org.eclipse.mylyn.internal.jira.core;

import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentSource;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.UnsubmittedTaskAttachment;

/**
 * @author Steffen Pingel
 */
public class JiraAttachmentHandler extends AbstractTaskAttachmentHandler {

	private final JiraRepositoryConnector connector;

	public JiraAttachmentHandler(JiraRepositoryConnector connector) {
		this.connector = connector;
	}

	@Override
	public InputStream getContent(TaskRepository repository, ITask task, TaskAttribute attachmentAttribute,
			IProgressMonitor monitor) throws CoreException {
		TaskAttachmentMapper mapper = TaskAttachmentMapper.createFrom(attachmentAttribute);
		String filename = mapper.getFileName();
		if (filename == null || filename.length() == 0) {
			throw new CoreException(new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR,
					JiraCorePlugin.ID_PLUGIN, RepositoryStatus.ERROR_REPOSITORY, "Attachment download from " //$NON-NLS-1$
							+ repository.getRepositoryUrl() + " failed, missing attachment filename.")); //$NON-NLS-1$
		}

		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(Messages.JiraAttachmentHandler_Downloading_attachment, IProgressMonitor.UNKNOWN);
			IJiraClient client = connector.getClientManager().getJiraClient(repository);
			int id = Integer.parseInt(task.getTaskId());
			return client.getAttachmentData(id, filename, monitor);
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		} finally {
			monitor.done();
		}
	}

	@Override
	public void postContent(TaskRepository repository, ITask task, AbstractTaskAttachmentSource source, String comment,
			TaskAttribute attachmentAttribute, IProgressMonitor monitor) throws CoreException {
		if (!JiraRepositoryConnector.hasAttachmentSupport(repository, task)) {
			throw new CoreException(new RepositoryStatus(repository.getRepositoryUrl(), IStatus.INFO,
					JiraCorePlugin.ID_PLUGIN, RepositoryStatus.ERROR_REPOSITORY,
					"Attachments are not supported by this repository access type")); //$NON-NLS-1$
		}

		UnsubmittedTaskAttachment attachment = new UnsubmittedTaskAttachment(source, attachmentAttribute);
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(Messages.JiraAttachmentHandler_Uploading_attachment, IProgressMonitor.UNKNOWN);
			try {
				IJiraClient client = connector.getClientManager().getJiraClient(repository);
				int id = Integer.parseInt(task.getTaskId());
				client.putAttachmentData(id, attachment.getFileName(), attachment.getDescription(),
						attachment.createInputStream(monitor), monitor, attachment.getReplaceExisting());
				if (comment != null && comment.length() > 0) {
					JiraTicket ticket = new JiraTicket(id);
					client.updateTicket(ticket, comment, monitor);
				}
			} catch (OperationCanceledException e) {
				throw e;
			} catch (Exception e) {
				throw new CoreException(JiraCorePlugin.toStatus(e, repository));
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean canGetContent(TaskRepository repository, ITask task) {
		return JiraRepositoryConnector.hasAttachmentSupport(repository, task);
	}

	@Override
	public boolean canPostContent(TaskRepository repository, ITask task) {
		return JiraRepositoryConnector.hasAttachmentSupport(repository, task);
	}

}
