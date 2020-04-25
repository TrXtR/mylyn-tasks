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
import org.eclipse.mylyn.tasks.ui.TaskRepositoryLocationUiFactory;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class JiraUiPlugin extends AbstractUIPlugin {

	public static final String ID_PLUGIN = "org.eclipse.mylyn.jira.ui"; //$NON-NLS-1$

	private static JiraUiPlugin plugin;

	public JiraUiPlugin() {
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		JiraCorePlugin.getDefault()
				.getConnector()
				.setTaskRepositoryLocationFactory(new TaskRepositoryLocationUiFactory());
		TasksUi.getRepositoryManager().addListener(JiraCorePlugin.getDefault().getConnector().getClientManager());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		TasksUi.getRepositoryManager().removeListener(JiraCorePlugin.getDefault().getConnector().getClientManager());

		plugin = null;
		super.stop(context);
	}

	public static JiraUiPlugin getDefault() {
		return plugin;
	}

}
