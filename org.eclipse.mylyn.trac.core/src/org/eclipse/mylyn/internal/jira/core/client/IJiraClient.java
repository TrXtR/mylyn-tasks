/*******************************************************************************
 * Copyright (c) 2006, 2010 Steffen Pingel and others.
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

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.internal.jira.core.model.JiraComment;
import org.eclipse.mylyn.internal.jira.core.model.JiraComponent;
import org.eclipse.mylyn.internal.jira.core.model.JiraMilestone;
import org.eclipse.mylyn.internal.jira.core.model.JiraPriority;
import org.eclipse.mylyn.internal.jira.core.model.JiraRepositoryInfo;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.internal.jira.core.model.JiraSeverity;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketField;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketResolution;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketStatus;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicketType;
import org.eclipse.mylyn.internal.jira.core.model.JiraVersion;

/**
 * Defines the requirements for classes that provide remote access to Jira repositories.
 *
 * @author Steffen Pingel
 */
public interface IJiraClient {

	public enum Version {
		XML_RPC, JIRA_0_9;

		public static Version fromVersion(String version) {
			try {
				return Version.valueOf(version);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		@Override
		public String toString() {
			switch (this) {
			case JIRA_0_9:
				return "Web"; //$NON-NLS-1$
			case XML_RPC:
				return "XML-RPC"; //$NON-NLS-1$
			default:
				return null;
			}
		}

	}

	public static final String CHARSET = "UTF-8"; //$NON-NLS-1$

	public static final String TIME_ZONE = "UTC"; //$NON-NLS-1$

	public static final String LOGIN_URL = "/login"; //$NON-NLS-1$

	public static final String QUERY_URL = "/query?format=tab"; //$NON-NLS-1$

	public static final String TICKET_URL = "/ticket/"; //$NON-NLS-1$

	public static final String NEW_TICKET_URL = "/newticket"; //$NON-NLS-1$

	public static final String CUSTOM_QUERY_URL = "/query"; //$NON-NLS-1$

	public static final String TICKET_ATTACHMENT_URL = "/attachment/ticket/"; //$NON-NLS-1$

	public static final String DEFAULT_USERNAME = "anonymous"; //$NON-NLS-1$

	public static final String WIKI_URL = "/wiki/"; //$NON-NLS-1$

	public static final String REPORT_URL = "/report/"; //$NON-NLS-1$

	public static final String CHANGESET_URL = "/changeset/"; //$NON-NLS-1$

	public static final String REVISION_LOG_URL = "/log/"; //$NON-NLS-1$

	public static final String MILESTONE_URL = "/milestone/"; //$NON-NLS-1$

	public static final String BROWSER_URL = "/browser/"; //$NON-NLS-1$

	public static final String ATTACHMENT_URL = "/attachment/ticket/"; //$NON-NLS-1$

	/**
	 * Gets ticket with <code>id</code> from repository.
	 *
	 * @param id
	 *            the id of the ticket to get
	 * @param monitor
	 *            TODO
	 * @return the ticket
	 * @throws JiraException
	 *             thrown in case of a connection error
	 */
	JiraTicket getTicket(int id, IProgressMonitor monitor) throws JiraException;

	/**
	 * Returns the access type.
	 */
	Version getAccessMode();

	/**
	 * Returns the repository url.
	 */
	String getUrl();

	/**
	 * Queries tickets from repository. All found tickets are added to <code>result</code>.
	 *
	 * @param query
	 *            the search criteria
	 * @param result
	 *            the list of found tickets
	 * @throws JiraException
	 *             thrown in case of a connection error
	 */
	void search(JiraSearch query, List<JiraTicket> result, IProgressMonitor monitor) throws JiraException;

	/**
	 * Queries ticket id from repository. All found tickets are added to <code>result</code>.
	 *
	 * @param query
	 *            the search criteria
	 * @param result
	 *            the list of found tickets
	 * @throws JiraException
	 *             thrown in case of a connection error
	 */
	void searchForTicketIds(JiraSearch query, List<Integer> result, IProgressMonitor monitor) throws JiraException;

	/**
	 * Validates the repository connection.
	 *
	 * @return information about the repository
	 * @throws JiraException
	 *             thrown in case of a connection error
	 */
	JiraRepositoryInfo validate(IProgressMonitor monitor) throws JiraException;

	/**
	 * Returns true, if the repository details are cached. If this method returns true, invoking
	 * <tt>updateAttributes(monitor, false)</tt> will return without opening a connection.
	 *
	 * @see #updateAttributes(IProgressMonitor, boolean)
	 */
	boolean hasAttributes();

	/**
	 * Updates cached repository details: milestones, versions etc.
	 *
	 * @throws JiraException
	 *             thrown in case of a connection error
	 */
	void updateAttributes(IProgressMonitor monitor, boolean force) throws JiraException;

	JiraComponent[] getComponents();

	JiraTicketField[] getTicketFields();

	JiraTicketField getTicketFieldByName(String jiraKey);

	JiraMilestone[] getMilestones();

	JiraPriority[] getPriorities();

	JiraSeverity[] getSeverities();

	JiraTicketResolution[] getTicketResolutions();

	JiraTicketStatus[] getTicketStatus();

	JiraTicketType[] getTicketTypes();

	JiraVersion[] getVersions();

	InputStream getAttachmentData(int ticketId, String filename, IProgressMonitor monitor) throws JiraException;

	void putAttachmentData(int ticketId, String name, String description, InputStream source, IProgressMonitor monitor,
			boolean replace) throws JiraException;

	void deleteAttachment(int ticketId, String filename, IProgressMonitor monitor) throws JiraException;

	/**
	 * @return the id of the created ticket
	 */
	int createTicket(JiraTicket ticket, IProgressMonitor monitor) throws JiraException;

	void updateTicket(JiraTicket ticket, String comment, IProgressMonitor monitor) throws JiraException;

	/**
	 * Sets a reference to the cached repository attributes.
	 *
	 * @param data
	 *            cached repository attributes
	 */
	void setData(JiraClientData data);

	Set<Integer> getChangedTickets(Date since, IProgressMonitor monitor) throws JiraException;

	Date getTicketLastChanged(Integer id, IProgressMonitor monitor) throws JiraException;

	void deleteTicket(int ticketId, IProgressMonitor monitor) throws JiraException;

	List<JiraComment> getComments(int id, IProgressMonitor monitor) throws JiraException;

}
