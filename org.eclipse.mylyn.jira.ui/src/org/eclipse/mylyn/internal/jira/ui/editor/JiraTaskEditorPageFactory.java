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

package org.eclipse.mylyn.internal.jira.ui.editor;

import org.eclipse.mylyn.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.JiraRepositoryConnector;
import org.eclipse.mylyn.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPageFactory;
import org.eclipse.mylyn.tasks.ui.editors.BrowserFormPage;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.FormPage;

/**
 * @author Steffen Pingel
 */
public class JiraTaskEditorPageFactory extends AbstractTaskEditorPageFactory {

	@Override
	public boolean canCreatePageFor(TaskEditorInput input) {
		if (input.getTask().getConnectorKind().equals(JiraCorePlugin.CONNECTOR_KIND)) {
			return true;
		} else if (TasksUiUtil.isOutgoingNewTask(input.getTask(), JiraCorePlugin.CONNECTOR_KIND)) {
			return true;
		}
		return false;
	}

	@Override
	public FormPage createPage(TaskEditor parentEditor) {
		TaskEditorInput input = parentEditor.getTaskEditorInput();
		if (TasksUiUtil.isOutgoingNewTask(input.getTask(), JiraCorePlugin.CONNECTOR_KIND)) {
			return new JiraTaskEditorPage(parentEditor);
		} else if (JiraRepositoryConnector.hasRichEditor(input.getTaskRepository())) {
			return new JiraTaskEditorPage(parentEditor);
		} else {
			return new BrowserFormPage(parentEditor, Messages.JiraTaskEditorPageFactory_Browser);
		}
	}

	@Override
	public String[] getConflictingIds(TaskEditorInput input) {
		if (JiraRepositoryConnector.hasRichEditor(input.getTaskRepository())
				|| TasksUiUtil.isOutgoingNewTask(input.getTask(), JiraCorePlugin.CONNECTOR_KIND)) {
			return new String[] { ITasksUiConstants.ID_PAGE_PLANNING };
		} else {
			return super.getConflictingIds(input);
		}
	}

	@Override
	public Image getPageImage() {
		return CommonImages.getImage(TasksUiImages.REPOSITORY_SMALL);
	}

	@Override
	public String getPageText() {
		return "Jira"; //$NON-NLS-1$
	}

	@Override
	public int getPriority() {
		return PRIORITY_TASK;
	}

}
