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

package org.eclipse.mylyn.internal.jira.core;

import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;

/**
 * Provides a mapping from Mylyn task keys to Jira ticket keys.
 *
 * @author Steffen Pingel
 */
public class JiraAttributeMapper extends TaskAttributeMapper {

	public enum Flag {
		READ_ONLY, ATTRIBUTE, PEOPLE
	};

	public static final String NEW_CC = "task.common.newcc"; //$NON-NLS-1$

	public static final String REMOVE_CC = "task.common.removecc"; //$NON-NLS-1$

	public static final EnumSet<Flag> NO_FLAGS = EnumSet.noneOf(Flag.class);

	private final IJiraClient client;

	public static boolean isInternalAttribute(TaskAttribute attribute) {
		String type = attribute.getMetaData().getType();
		if (TaskAttribute.TYPE_ATTACHMENT.equals(type) || TaskAttribute.TYPE_OPERATION.equals(type)
				|| TaskAttribute.TYPE_COMMENT.equals(type)) {
			return true;
		}
		String id = attribute.getId();
		return TaskAttribute.COMMENT_NEW.equals(id) || TaskAttribute.ADD_SELF_CC.equals(id) || REMOVE_CC.equals(id)
				|| NEW_CC.equals(id);
	}

	public JiraAttributeMapper(TaskRepository taskRepository, IJiraClient client) {
		super(taskRepository);
		Assert.isNotNull(client);
		this.client = client;
	}

	@Override
	public Date getDateValue(TaskAttribute attribute) {
		return JiraUtil.parseDate(attribute.getValue());
	}

	@Override
	public String mapToRepositoryKey(TaskAttribute parent, String key) {
		JiraAttribute attribute = JiraAttribute.getByTaskKey(key);
		return (attribute != null) ? attribute.getJiraKey() : key;
	}

	@Override
	public void setDateValue(TaskAttribute attribute, Date date) {
		if (date == null) {
			attribute.clearValues();
		} else {
			attribute.setValue(JiraUtil.toJiraTime(date) + ""); //$NON-NLS-1$
		}
	}

	@Override
	public Map<String, String> getOptions(TaskAttribute attribute) {
		Map<String, String> options = getRepositoryOptions(client, attribute.getId());
		return (options != null) ? options : super.getOptions(attribute);
	}

	public static Map<String, String> getRepositoryOptions(IJiraClient client, String jirakKey) {
		if (client.hasAttributes()) {
			if (JiraAttribute.STATUS.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getTicketStatus(), false);
			} else if (JiraAttribute.RESOLUTION.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getTicketResolutions(), false);
			} else if (JiraAttribute.COMPONENT.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getComponents(), false);
			} else if (JiraAttribute.VERSION.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getVersions(), true);
			} else if (JiraAttribute.PRIORITY.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getPriorities(), false);
			} else if (JiraAttribute.SEVERITY.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getSeverities(), false);
			} else if (JiraAttribute.MILESTONE.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getMilestones(), true);
			} else if (JiraAttribute.TYPE.getJiraKey().equals(jirakKey)) {
				return getOptions(client.getTicketTypes(), false);
			}
		}
		return null;
	}

	private static Map<String, String> getOptions(Object[] values, boolean allowEmpty) {
		if (values != null && values.length > 0) {
			Map<String, String> options = new LinkedHashMap<String, String>();
			if (allowEmpty) {
				options.put("", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (Object value : values) {
				options.put(value.toString(), value.toString());
			}
			return options;
		}
		return null;
	}

}
