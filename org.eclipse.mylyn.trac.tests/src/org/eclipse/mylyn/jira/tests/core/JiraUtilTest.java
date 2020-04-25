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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearchFilter;
import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * @author Steffen Pingel
 */
public class JiraUtilTest extends TestCase {

	private TaskRepository taskRepository;

	@Override
	protected void setUp() throws Exception {
		taskRepository = new TaskRepository(JiraCorePlugin.CONNECTOR_KIND, "http://mylyn.org/jira");
	}

	public void testToJiraSearch() {
		String queryParameter = "&order=priority&status=new&status=assigned&status=reopened&milestone=M1&owner=%7E%C3%A4%C3%B6%C3%BC";
		IRepositoryQuery query = TasksUi.getRepositoryModel().createRepositoryQuery(taskRepository);
		query.setUrl(taskRepository.getRepositoryUrl() + IJiraClient.QUERY_URL + queryParameter);

		JiraSearch search = JiraUtil.toJiraSearch(query);
		assertNotNull(search);
		assertEquals(queryParameter, search.toUrl());
	}

	public void testToJiraSearchFilterList() {
		String parameterUrl = "&status=new&status=assigned&status=reopened&milestone=0.1";
		String queryUrl = taskRepository.getRepositoryUrl() + IJiraClient.QUERY_URL + parameterUrl;
		IRepositoryQuery query = TasksUi.getRepositoryModel().createRepositoryQuery(taskRepository);
		query.setUrl(queryUrl);

		JiraSearch filterList = JiraUtil.toJiraSearch(query);
		assertEquals(parameterUrl, filterList.toUrl());
		assertEquals("&status=new|assigned|reopened&milestone=0.1", filterList.toQuery());

		List<JiraSearchFilter> list = filterList.getFilters();
		JiraSearchFilter filter = list.get(0);
		assertEquals("status", filter.getFieldName());
		assertEquals(Arrays.asList("new", "assigned", "reopened"), filter.getValues());
		filter = list.get(1);
		assertEquals("milestone", filter.getFieldName());
		assertEquals(Arrays.asList("0.1"), filter.getValues());
	}

	public void testEncodeUrl() {
		assertEquals("encode", JiraUtil.encodeUrl("encode"));
		assertEquals("sp%20ace%20", JiraUtil.encodeUrl("sp ace "));
		assertEquals("%2B%2B", JiraUtil.encodeUrl("++"));
		assertEquals("%2520", JiraUtil.encodeUrl("%20"));
		assertEquals("%2Fslash", JiraUtil.encodeUrl("/slash"));
	}

}
