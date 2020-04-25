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

package org.eclipse.mylyn.internal.jira.ui;

import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearchFilter;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearchFilter.CompareOperator;
import org.eclipse.mylyn.internal.tasks.core.AbstractSearchHandler;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskData;

@SuppressWarnings("restriction")
public class JiraSearchHandler extends AbstractSearchHandler {

	@Override
	public String getConnectorKind() {
		return JiraCorePlugin.CONNECTOR_KIND;
	}

	@Override
	public boolean queryForText(TaskRepository taskRepository, IRepositoryQuery query, TaskData taskData,
			String searchString) {
		JiraSearchFilter filter = new JiraSearchFilter("description"); //$NON-NLS-1$
		filter.setOperator(CompareOperator.CONTAINS);
		filter.addValue(searchString);

		JiraSearch search = new JiraSearch();
		search.addFilter(filter);

		// TODO copied from JiraQueryPage.getQueryUrl()
		StringBuilder sb = new StringBuilder();
		sb.append(taskRepository.getRepositoryUrl());
		sb.append(IJiraClient.QUERY_URL);
		sb.append(search.toUrl());

		query.setUrl(sb.toString());

		return true;
	}

}
