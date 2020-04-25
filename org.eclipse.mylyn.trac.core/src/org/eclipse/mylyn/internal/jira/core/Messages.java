/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.mylyn.internal.jira.core.messages"; //$NON-NLS-1$

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String JiraAttachmentHandler_Downloading_attachment;

	public static String JiraAttachmentHandler_Uploading_attachment;

	public static String JiraAttribute_Assigned_to;

	public static String JiraAttribute_CC;

	public static String JiraAttribute_Component;

	public static String JiraAttribute_Created;

	public static String JiraAttribute_Description;

	public static String JiraAttribute_ID;

	public static String JiraAttribute_Keywords;

	public static String JiraAttribute_Last_Modification;

	public static String JiraAttribute_Milestone;

	public static String JiraAttribute_Priority;

	public static String JiraAttribute_Reporter;

	public static String JiraAttribute_Resolution;

	public static String JiraAttribute_Severity;

	public static String JiraAttribute_Status;

	public static String JiraAttribute_Summary;

	public static String JiraAttribute_Type;

	public static String JiraAttribute_Version;

	public static String JiraCorePlugin_I_O_error_has_occured;

	public static String JiraCorePlugin_Repository_URL_is_invalid;

	public static String JiraCorePlugin_the_SERVER_RETURNED_an_UNEXPECTED_RESOPNSE;

	public static String JiraCorePlugin_Unexpected_error;

	public static String JiraCorePlugin_Unexpected_server_response_;

	public static String JiraRepositoryConnector_Getting_changed_tasks;

	public static String JiraRepositoryConnector_Querying_repository;

	public static String JiraRepositoryConnector_Jira_Client_Label;

	public static String JiraTaskDataHandler_Accept;

	public static String JiraTaskDataHandler_Leave;

	public static String JiraTaskDataHandler_Leave_as_Status;

	public static String JiraTaskDataHandler_Leave_as_Status_Resolution;

	public static String JiraTaskDataHandler_Reopen;

	public static String JiraTaskDataHandler_Resolve_as;

	public static String JiraWikiHandler_Download_Wiki_Page;

	public static String JiraWikiHandler_Download_Wiki_Page_Names;

	public static String JiraWikiHandler_Retrieve_Wiki_Page_History;

	public static String JiraWikiHandler_Upload_Wiki_Page;
}
