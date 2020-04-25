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

package org.eclipse.mylyn.internal.jira.ui.wizard;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.mylyn.internal.jira.ui.wizard.messages"; //$NON-NLS-1$

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String JiraFilterQueryPage_Add_search_filters_to_define_query;

	public static String JiraFilterQueryPage_CC;

	public static String JiraFilterQueryPage_Component;

	public static String JiraFilterQueryPage_Keywords;

	public static String JiraFilterQueryPage_Milestone;

	public static String JiraFilterQueryPage_New_Jira_Query;

	public static String JiraFilterQueryPage_or;

	public static String JiraFilterQueryPage_Owner;

	public static String JiraFilterQueryPage_Priority;

	public static String JiraFilterQueryPage_Query_Title;

	public static String JiraFilterQueryPage_Reporter;

	public static String JiraFilterQueryPage_Resolution;

	public static String JiraFilterQueryPage_Select_to_add_filter;

	public static String JiraFilterQueryPage_Status;

	public static String JiraFilterQueryPage_Summary;

	public static String JiraFilterQueryPage_Type;

	public static String JiraFilterQueryPage_Version;

	public static String JiraQueryPage_CC;

	public static String JiraQueryPage_Component;

	public static String JiraQueryPage_Description;

	public static String JiraQueryPage_Enter_query_parameters;

	public static String JiraQueryPage_If_attributes_are_blank_or_stale_press_the_Update_button;

	public static String JiraQueryPage_Keywords;

	public static String JiraQueryPage_Milestone;

	public static String JiraQueryPage_Owner;

	public static String JiraQueryPage_Priority;

	public static String JiraQueryPage_Reporter;

	public static String JiraQueryPage_Resolution;

	public static String JiraQueryPage_Status;

	public static String JiraQueryPage_Summary;

	public static String JiraQueryPage_Type;

	public static String JiraQueryPage_Version;

	public static String JiraRepositorySettingsPage_Access_Type_;

	public static String JiraRepositorySettingsPage_auth_failed_missing_credentials;

	public static String JiraRepositorySettingsPage_Authentication_credentials_are_valid;

	public static String JiraRepositorySettingsPage_Authentication_credentials_valid_Update_to_latest_XmlRpcPlugin_Warning;

	public static String JiraRepositorySettingsPage_Automatic__Use_Validate_Settings_;

	public static String JiraRepositorySettingsPage_EXAMPLE_HTTP_JIRA_EDGEWALL_ORG;

	public static String JiraRepositorySettingsPage_No_Jira_repository_found_at_url;

	public static String JiraRepositorySettingsPage_NTLM_authentication_requested_Error;

	public static String JiraRepositorySettingsPage_Jira_Repository_Settings;
}
