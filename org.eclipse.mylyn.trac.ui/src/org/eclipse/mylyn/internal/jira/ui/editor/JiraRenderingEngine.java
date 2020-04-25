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

package org.eclipse.mylyn.internal.jira.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.JiraException;
import org.eclipse.mylyn.internal.jira.core.client.JiraXmlRpcClient;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.editors.AbstractRenderingEngine;

/**
 * @author Xiaoyang Guan
 * @since 2.1
 */
public class JiraRenderingEngine extends AbstractRenderingEngine {

	/**
	 * generate HTML preview page for <code>text</code>
	 */
	@Override
	public String renderAsHtml(TaskRepository repository, String text, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Render HTML Preview", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		try {
			IJiraClient client = JiraCorePlugin.getDefault()
					.getConnector()
					.getClientManager()
					.getJiraClient(repository);
			if (client instanceof JiraXmlRpcClient) {
				JiraXmlRpcClient jiraXmlRpcClient = (JiraXmlRpcClient) client;
				String htmlText = jiraXmlRpcClient.wikiToHtml(text, monitor);
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}

				String htmlHeader = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" //$NON-NLS-1$
						+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">" //$NON-NLS-1$
						+ "<head>" //$NON-NLS-1$
						+ "<link rel=\"stylesheet\" href=\"REPOSITORY_URL/chrome/common/css/jira.css\" type=\"text/css\" />" //$NON-NLS-1$
						+ "<link rel=\"stylesheet\" href=\"REPOSITORY_URL/chrome/common/css/wiki.css\" type=\"text/css\" />" //$NON-NLS-1$
						+ "<link rel=\"icon\" href=\"REPOSITORY_URL/chrome/common/jira.ico\" type=\"image/x-icon\" />" //$NON-NLS-1$
						+ "<link rel=\"shortcut icon\" href=\"EPOSITORY_URL/chrome/common/jira.ico\" type=\"image/x-icon\" />" //$NON-NLS-1$
						+ "<style type=\"text/css\">body {background: #f4f4f4 url(REPOSITORY_URL/chrome/common/draft.png);margin: 0;padding: 0;}</style>" //$NON-NLS-1$
						+ "</head>"; //$NON-NLS-1$
				String htmlBody = "<body> " + htmlText + "</body>"; //$NON-NLS-1$ //$NON-NLS-2$
				String htmlFooter = "</html>"; //$NON-NLS-1$

				String html = htmlHeader.replace("REPOSITORY_URL", repository.getRepositoryUrl()) + htmlBody //$NON-NLS-1$
						+ htmlFooter;
				return html;
			} else {
				throw new CoreException(JiraCorePlugin
						.toStatus(new JiraException("Preview is available only in XML-RPC access mode"), repository)); //$NON-NLS-1$
			}
		} catch (JiraException e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		} finally {
			monitor.done();
		}
	}

}
