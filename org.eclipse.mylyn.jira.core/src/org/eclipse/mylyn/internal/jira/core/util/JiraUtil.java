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
 *     Jorrit Schippers - fix for bug 254862
 *******************************************************************************/

package org.eclipse.mylyn.internal.jira.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;

/**
 * Provides static helper methods.
 * 
 * @author Steffen Pingel
 */
public class JiraUtil {

	public static Date parseDate(String time) {
		if (time != null) {
			try {
				return JiraUtil.parseDate(Long.valueOf(time));
			} catch (NumberFormatException e) {
			}
		}
		return null;
	}

	public static Date parseDate(long seconds) {
		return new Date(seconds * 1000l);
//		Calendar c = Calendar.getInstance();
//		c.setTimeZone(TimeZone.getTimeZone(IJiraClient.TIME_ZONE));
//		c.setTimeInMillis(seconds * 1000l);
//		return c.getTime();
	}

	public static long toJiraTime(Date date) {
//		Calendar c = Calendar.getInstance();
//		c.setTime(date);
//		c.setTimeZone(TimeZone.getTimeZone(IJiraClient.TIME_ZONE));
//		return c.getTimeInMillis() / 1000l;
		return date.getTime() / 1000l;
	}

	private static String getQueryParameter(IRepositoryQuery query) {
		String url = query.getUrl();
		int i = url.indexOf(IJiraClient.QUERY_URL);
		if (i == -1) {
			return null;
		}
		return url.substring(i + IJiraClient.QUERY_URL.length());
	}

	/**
	 * Creates a <code>JiraSearch</code> object from this query.
	 */
	public static JiraSearch toJiraSearch(IRepositoryQuery query) {
		String url = getQueryParameter(query);
		if (url != null) {
			JiraSearch search = new JiraSearch();
			search.fromUrl(url);
			return search;
		}
		return null;
	}

	public static IStatus createPermissionDeniedError(String repositoryUrl, String pluginId) {
		return new RepositoryStatus(repositoryUrl, IStatus.ERROR, JiraCorePlugin.ID_PLUGIN,
				RepositoryStatus.ERROR_PERMISSION_DENIED, Messages.JiraUtil_Permission_denied);
	}

	public static String encodeUrl(String string) {
		try {
			return URLEncoder.encode(string, IJiraClient.CHARSET).replaceAll("\\+", "%20"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (UnsupportedEncodingException e) {
			return string;
		}
	}

}
