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

package org.eclipse.mylyn.internal.jira.core.client;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.eclipse.mylyn.internal.jira.core.model.JiraComponent;
import org.eclipse.mylyn.internal.jira.core.model.JiraMilestone;
import org.eclipse.mylyn.internal.jira.core.model.JiraPriority;
import org.eclipse.mylyn.internal.jira.core.model.JiraSeverity;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketField;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketResolution;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketStatus;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketType;
import org.eclipse.mylyn.internal.jira.core.model.JiraVersion;

public class JiraClientData implements Serializable {

	private static final long serialVersionUID = 6891961984245981675L;

	List<JiraComponent> components;

	List<JiraMilestone> milestones;

	List<JiraPriority> priorities;

	List<JiraSeverity> severities;

	List<JiraTicketField> ticketFields;

	List<JiraTicketResolution> ticketResolutions;

	List<JiraTicketStatus> ticketStatus;

	List<JiraTicketType> ticketTypes;

	List<JiraVersion> versions;

	long lastUpdate;

	transient Map<String, JiraTicketField> ticketFieldByName;

}
