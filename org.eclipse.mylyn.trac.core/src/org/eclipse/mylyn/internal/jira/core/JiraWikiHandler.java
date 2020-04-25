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
 *     Xiaoyang Guan - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.jira.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.internal.jira.core.client.AbstractWikiHandler;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.IJiraWikiClient;
import org.eclipse.mylyn.internal.jira.core.client.JiraException;
import org.eclipse.mylyn.internal.jira.core.model.JiraWikiPage;
import org.eclipse.mylyn.internal.jira.core.model.JiraWikiPageInfo;
import org.eclipse.mylyn.tasks.core.TaskRepository;

/**
 * @author Xiaoyang Guan
 */
public class JiraWikiHandler extends AbstractWikiHandler {

	private final JiraRepositoryConnector connector;

	public JiraWikiHandler(JiraRepositoryConnector connector) {
		this.connector = connector;
	}

	@Override
	public String[] downloadAllPageNames(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(Messages.JiraWikiHandler_Download_Wiki_Page_Names, IProgressMonitor.UNKNOWN);
		try {
			String[] names = getJiraWikiClient(repository).getAllWikiPageNames(monitor);
			return names;
		} catch (JiraException e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		} finally {
			monitor.done();
		}
	}

	@Override
	public JiraWikiPage getWikiPage(TaskRepository repository, String pageName, IProgressMonitor monitor)
			throws CoreException {
		monitor.beginTask(Messages.JiraWikiHandler_Download_Wiki_Page, IProgressMonitor.UNKNOWN);
		try {
			JiraWikiPage page = getJiraWikiClient(repository).getWikiPage(pageName, monitor);
			return page;
		} catch (JiraException e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		} finally {
			monitor.done();
		}
	}

	@Override
	public void postWikiPage(TaskRepository repository, JiraWikiPage newPage, IProgressMonitor monitor)
			throws CoreException {
		monitor.beginTask(Messages.JiraWikiHandler_Upload_Wiki_Page, IProgressMonitor.UNKNOWN);
		try {
			String pageName = newPage.getPageInfo().getPageName();
			String content = newPage.getContent();
			Map<String, Object> attributes = new HashMap<String, Object>();
			attributes.put("comment", newPage.getPageInfo().getComment()); //$NON-NLS-1$
			attributes.put("author", newPage.getPageInfo().getAuthor()); //$NON-NLS-1$
			boolean success = getJiraWikiClient(repository).putWikipage(pageName, content, attributes, monitor);
			if (success) {
				return;
			} else {
				throw new CoreException(JiraCorePlugin.toStatus(new JiraException(
						"Failed to upload wiki page. No further information available."), repository)); //$NON-NLS-1$
			}
		} catch (JiraException e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		} finally {
			monitor.done();
		}
	}

	@Override
	public JiraWikiPageInfo[] getPageHistory(TaskRepository repository, String pageName, IProgressMonitor monitor)
			throws CoreException {
		monitor.beginTask(Messages.JiraWikiHandler_Retrieve_Wiki_Page_History, IProgressMonitor.UNKNOWN);
		try {
			JiraWikiPageInfo[] versions = getJiraWikiClient(repository).getWikiPageInfoAllVersions(pageName, monitor);
			return versions;
		} catch (JiraException e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		} finally {
			monitor.done();
		}
	}

	private IJiraWikiClient getJiraWikiClient(TaskRepository repository) throws JiraException {
		IJiraClient client = connector.getClientManager().getJiraClient(repository);
		if (client instanceof IJiraWikiClient) {
			return (IJiraWikiClient) client;
		} else {
			throw new JiraException("The access mode of " + repository.toString() //$NON-NLS-1$
					+ " does not support Wiki page editting."); //$NON-NLS-1$
		}
	}

	@Override
	public String getWikiUrl(TaskRepository repository) {
		return repository.getRepositoryUrl() + IJiraClient.WIKI_URL;
	}
}
