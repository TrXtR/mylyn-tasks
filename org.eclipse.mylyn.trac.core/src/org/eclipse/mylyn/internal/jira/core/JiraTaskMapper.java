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

import org.eclipse.mylyn.internal.jira.core.JiraRepositoryConnector.TaskKind;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraPriority;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;

/**
 * @author Steffen Pingel
 */
public class JiraTaskMapper extends TaskMapper {

	private final IJiraClient client;

	public JiraTaskMapper(TaskData taskData, IJiraClient client) {
		super(taskData);
		this.client = client;
	}

//	@Override
//	public boolean applyTo(ITask task) {
//		boolean changed = false;
//		if (hasChanges(task.getCompletionDate(), TaskAttribute.DATE_COMPLETION)) {
//			task.setCompletionDate(getCompletionDate());
//			changed = true;
//		}
//		if (hasChanges(task.getCreationDate(), TaskAttribute.DATE_CREATION)) {
//			task.setCreationDate(getCreationDate());
//			changed = true;
//		}
//		if (hasChanges(task.getModificationDate(), TaskAttribute.DATE_MODIFICATION)) {
//			task.setModificationDate(getModificationDate());
//			changed = true;
//		}
//		if (hasChanges(task.getDueDate(), TaskAttribute.DATE_DUE)) {
//			task.setDueDate(getDueDate());
//			changed = true;
//		}
//		if (hasChanges(task.getOwner(), TaskAttribute.USER_ASSIGNED)) {
//			task.setOwner(getOwner());
//			changed = true;
//		}
//		if (hasChanges(task.getPriority(), TaskAttribute.PRIORITY)) {
//			if (getPriorityLevel() != null) {
//				task.setPriority(getPriorityLevel().toString());
//			} else {
//				task.setPriority(PriorityLevel.getDefault().toString());
//			}
//			changed = true;
//		}
//		if (hasChanges(task.getSummary(), TaskAttribute.SUMMARY)) {
//			task.setSummary(getSummary());
//			changed = true;
//		}
//		if (hasChanges(task.getTaskKey(), TaskAttribute.TASK_KEY)) {
//			task.setTaskKey(getTaskKey());
//			changed = true;
//		}
//		if (hasChanges(task.getTaskKind(), TaskAttribute.TASK_KIND)) {
//			task.setTaskKind(getTaskKind());
//			changed = true;
//		}
//		if (hasChanges(task.getUrl(), TaskAttribute.TASK_URL)) {
//			task.setUrl(getTaskUrl());
//			changed = true;
//		}
//		return changed;
//	}
//
//	@Override
//	public boolean hasChanges(ITask task) {
//		boolean changed = false;
//		changed |= hasChanges(task.getCompletionDate(), TaskAttribute.DATE_COMPLETION);
//		changed |= hasChanges(task.getCreationDate(), TaskAttribute.DATE_CREATION);
//		changed |= hasChanges(task.getModificationDate(), TaskAttribute.DATE_MODIFICATION);
//		changed |= hasChanges(task.getDueDate(), TaskAttribute.DATE_DUE);
//		changed |= hasChanges(task.getOwner(), TaskAttribute.USER_ASSIGNED);
//		changed |= hasChanges(task.getPriority(), TaskAttribute.PRIORITY);
//		changed |= hasChanges(task.getSummary(), TaskAttribute.SUMMARY);
//		changed |= hasChanges(task.getTaskKey(), TaskAttribute.TASK_KEY);
//		changed |= hasChanges(task.getTaskKind(), TaskAttribute.TASK_KIND);
//		changed |= hasChanges(task.getUrl(), TaskAttribute.TASK_URL);
//		return changed;
//	}
//
//	private boolean hasChanges(Object value, String attributeKey) {
//		TaskData taskData = getTaskData();
//		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeKey);
//		if (attribute != null) {
//			if (TaskAttribute.TYPE_BOOLEAN.equals(attribute.getMetaData().getType())) {
//				return areNotEquals(value, taskData.getAttributeMapper().getBooleanValue(attribute));
//			} else if (TaskAttribute.TYPE_DATE.equals(attribute.getMetaData().getType())) {
//				return areNotEquals(value, taskData.getAttributeMapper().getDateValue(attribute));
//			} else if (TaskAttribute.TYPE_INTEGER.equals(attribute.getMetaData().getType())) {
//				return areNotEquals(value, taskData.getAttributeMapper().getIntegerValue(attribute));
//			} else if (TaskAttribute.PRIORITY.equals(attributeKey)) {
//				PriorityLevel priorityLevel = getPriorityLevel();
//				return areNotEquals(value, (priorityLevel != null) ? priorityLevel.toString() : getPriority());
//			} else if (TaskAttribute.TASK_KIND.equals(attributeKey)) {
//				return areNotEquals(value, getTaskKind());
//			} else {
//				return areNotEquals(value, taskData.getAttributeMapper().getValue(attribute));
//			}
//		}
//		return false;
//	}
//
//	private boolean areNotEquals(Object existingProperty, Object newProperty) {
//		return (existingProperty != null) ? !existingProperty.equals(newProperty) : newProperty != null;
//	}

	@Override
	public PriorityLevel getPriorityLevel() {
		String priority = getPriority();
		if (priority != null) {
			if (client != null) {
				JiraPriority[] jiraPriorities = client.getPriorities();
				return JiraRepositoryConnector.getTaskPriority(priority, jiraPriorities);
			} else {
				return JiraRepositoryConnector.getTaskPriority(priority);
			}
		}
		return null;
	}

	@Override
	public String getTaskKind() {
		String jiraTaskKind = super.getTaskKind();
		TaskKind taskKind = TaskKind.fromType(jiraTaskKind);
		return (taskKind != null) ? taskKind.toString() : jiraTaskKind;
	}

}
