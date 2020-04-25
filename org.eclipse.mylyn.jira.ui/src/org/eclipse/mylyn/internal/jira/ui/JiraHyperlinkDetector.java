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

import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractTaskHyperlinkDetector;

/**
 * @author Steffen Pingel
 */
public class JiraHyperlinkDetector extends AbstractTaskHyperlinkDetector {

	public JiraHyperlinkDetector() {
	}

	@Override
	protected List<IHyperlink> detectHyperlinks(ITextViewer textViewer, String content, int index, int contentOffset) {
		TaskRepository taskRepository = getTaskRepository(textViewer);
		if (taskRepository != null && JiraCorePlugin.CONNECTOR_KIND.equals(taskRepository.getConnectorKind())) {
			return JiraHyperlinkUtil.findJiraHyperlinks(taskRepository, content, index, contentOffset);
		}
		return null;
	}

}
