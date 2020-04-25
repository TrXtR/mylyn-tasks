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

package org.eclipse.mylyn.internal.jira.core;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.tasks.core.AbstractTaskListMigrator;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.w3c.dom.Element;

/**
 * @author Steffen Pingel
 */
public class JiraTaskListMigrator extends AbstractTaskListMigrator {

	private static final String KEY_JIRA = "Jira"; //$NON-NLS-1$

	private static final String KEY_JIRA_TASK = KEY_JIRA + KEY_TASK;

	private static final String KEY_JIRA_QUERY = KEY_JIRA + KEY_QUERY;

	private static final String KEY_SUPPORTS_SUBTASKS = "SupportsSubtasks"; //$NON-NLS-1$

	@Override
	public String getConnectorKind() {
		return JiraCorePlugin.CONNECTOR_KIND;
	}

	@Override
	public String getTaskElementName() {
		return KEY_JIRA_TASK;
	}

	@Override
	public Set<String> getQueryElementNames() {
		Set<String> names = new HashSet<String>();
		names.add(KEY_JIRA_QUERY);
		return names;
	}

	@Override
	public void migrateQuery(IRepositoryQuery query, Element element) {
		// nothing to do
	}

	@Override
	public void migrateTask(ITask task, Element element) {
		String lastModDate = element.getAttribute(KEY_LAST_MOD_DATE);
		task.setModificationDate(JiraUtil.parseDate(lastModDate));
		task.setAttribute(JiraRepositoryConnector.TASK_KEY_UPDATE_DATE, lastModDate);
		if (element.hasAttribute(KEY_SUPPORTS_SUBTASKS)) {
			task.setAttribute(JiraRepositoryConnector.TASK_KEY_SUPPORTS_SUBTASKS,
					Boolean.valueOf(element.getAttribute(KEY_SUPPORTS_SUBTASKS)).toString());
		}
	}

}
