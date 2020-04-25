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

package org.eclipse.mylyn.internal.jira.core.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.internal.jira.core.model.JiraWikiPage;
import org.eclipse.mylyn.internal.jira.core.model.JiraWikiPageInfo;
import org.eclipse.mylyn.tasks.core.TaskRepository;

/**
 * @author Xiaoyang Guan
 */
public abstract class AbstractWikiHandler {

	public abstract String[] downloadAllPageNames(TaskRepository repository, IProgressMonitor monitor)
			throws CoreException;

	public abstract String getWikiUrl(TaskRepository repository);

	public abstract JiraWikiPage getWikiPage(TaskRepository repository, String pageName, IProgressMonitor monitor)
			throws CoreException;

	public abstract void postWikiPage(TaskRepository repository, JiraWikiPage page, IProgressMonitor monitor)
			throws CoreException;

	public abstract JiraWikiPageInfo[] getPageHistory(TaskRepository repository, String pageName,
			IProgressMonitor monitor) throws CoreException;
}
