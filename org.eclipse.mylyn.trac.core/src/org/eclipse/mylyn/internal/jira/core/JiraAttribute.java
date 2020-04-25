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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.mylyn.internal.jira.core.JiraAttributeMapper.Flag;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket.Key;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

/**
 * @author Steffen Pingel
 */
public enum JiraAttribute {

	CC(Key.CC, Messages.JiraAttribute_CC, TaskAttribute.USER_CC, TaskAttribute.TYPE_SHORT_TEXT, Flag.PEOPLE),

	CHANGE_TIME(Key.CHANGE_TIME, Messages.JiraAttribute_Last_Modification, TaskAttribute.DATE_MODIFICATION,
			TaskAttribute.TYPE_DATE, Flag.READ_ONLY),

	COMPONENT(Key.COMPONENT, Messages.JiraAttribute_Component, TaskAttribute.PRODUCT, TaskAttribute.TYPE_SINGLE_SELECT,
			Flag.ATTRIBUTE),

	DESCRIPTION(Key.DESCRIPTION, Messages.JiraAttribute_Description, TaskAttribute.DESCRIPTION,
			TaskAttribute.TYPE_LONG_RICH_TEXT),

	ID(Key.ID, Messages.JiraAttribute_ID, TaskAttribute.TASK_KEY, TaskAttribute.TYPE_SHORT_TEXT, Flag.PEOPLE),

	KEYWORDS(Key.KEYWORDS, Messages.JiraAttribute_Keywords, TaskAttribute.KEYWORDS, TaskAttribute.TYPE_SHORT_TEXT,
			Flag.ATTRIBUTE),

	MILESTONE(Key.MILESTONE, Messages.JiraAttribute_Milestone, null, TaskAttribute.TYPE_SINGLE_SELECT, Flag.ATTRIBUTE),

	OWNER(Key.OWNER, Messages.JiraAttribute_Assigned_to, TaskAttribute.USER_ASSIGNED, TaskAttribute.TYPE_PERSON,
			Flag.PEOPLE),

	PRIORITY(Key.PRIORITY, Messages.JiraAttribute_Priority, TaskAttribute.PRIORITY, TaskAttribute.TYPE_SINGLE_SELECT,
			Flag.ATTRIBUTE),

	REPORTER(Key.REPORTER, Messages.JiraAttribute_Reporter, TaskAttribute.USER_REPORTER, TaskAttribute.TYPE_PERSON,
			Flag.READ_ONLY),

	RESOLUTION(Key.RESOLUTION, Messages.JiraAttribute_Resolution, TaskAttribute.RESOLUTION,
			TaskAttribute.TYPE_SINGLE_SELECT),

	SEVERITY(Key.SEVERITY, Messages.JiraAttribute_Severity, null, TaskAttribute.TYPE_SINGLE_SELECT, Flag.ATTRIBUTE),

	STATUS(Key.STATUS, Messages.JiraAttribute_Status, TaskAttribute.STATUS, TaskAttribute.TYPE_SHORT_TEXT),

	SUMMARY(Key.SUMMARY, Messages.JiraAttribute_Summary, TaskAttribute.SUMMARY, TaskAttribute.TYPE_SHORT_RICH_TEXT),

	TIME(Key.TIME, Messages.JiraAttribute_Created, TaskAttribute.DATE_CREATION, TaskAttribute.TYPE_DATE, Flag.READ_ONLY),

	TYPE(Key.TYPE, Messages.JiraAttribute_Type, TaskAttribute.TASK_KIND, TaskAttribute.TYPE_SINGLE_SELECT,
			Flag.ATTRIBUTE),

	VERSION(Key.VERSION, Messages.JiraAttribute_Version, null, TaskAttribute.TYPE_SINGLE_SELECT, Flag.ATTRIBUTE),

	TOKEN(Key.TOKEN, "Update Token", null, TaskAttribute.TYPE_SHORT_TEXT, Flag.READ_ONLY); //$NON-NLS-1$

	static Map<String, JiraAttribute> attributeByJiraKey = new HashMap<String, JiraAttribute>();

	static Map<String, String> JiraKeyByTaskKey = new HashMap<String, String>();

	private final String jiraKey;

	private final String prettyName;

	private final String taskKey;

	private final String type;

	private EnumSet<Flag> flags;

	public static JiraAttribute getByTaskKey(String taskKey) {
		for (JiraAttribute attribute : values()) {
			if (taskKey.equals(attribute.getTaskKey())) {
				return attribute;
			}
		}
		return null;
	}

	public static JiraAttribute getByJiraKey(String jiraKey) {
		for (JiraAttribute attribute : values()) {
			if (jiraKey.equals(attribute.getJiraKey())) {
				return attribute;
			}
		}
		return null;
	}

	JiraAttribute(Key jiraKey, String prettyName, String taskKey, String type, Flag firstFlag, Flag... moreFlags) {
		this.jiraKey = jiraKey.getKey();
		this.taskKey = taskKey;
		this.prettyName = prettyName;
		this.type = type;
		if (firstFlag == null) {
			this.flags = JiraAttributeMapper.NO_FLAGS;
		} else {
			this.flags = EnumSet.of(firstFlag, moreFlags);
		}
	}

	JiraAttribute(Key jiraKey, String prettyName, String taskKey, String type) {
		this(jiraKey, prettyName, taskKey, type, null);
	}

	public String getTaskKey() {
		return taskKey;
	}

	public String getJiraKey() {
		return jiraKey;
	}

	public String getKind() {
		if (flags.contains(Flag.ATTRIBUTE)) {
			return TaskAttribute.KIND_DEFAULT;
		} else if (flags.contains(Flag.PEOPLE)) {
			return TaskAttribute.KIND_PEOPLE;
		}
		return null;
	}

	public String getType() {
		return type;
	}

	public boolean isReadOnly() {
		return flags.contains(Flag.READ_ONLY);
	}

	@Override
	public String toString() {
		return prettyName;
	}

}
