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

package org.eclipse.mylyn.internal.jira.core.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearchFilter.CompareOperator;
import org.eclipse.osgi.util.NLS;

/**
 * Represents a Jira search. A search can have multiple {@link JiraSearchFilter}s that all need to match.
 * 
 * @author Steffen Pingel
 */
public class JiraSearch {

	/** Stores search criteria in the order entered by the user. */
	private final Map<String, JiraSearchFilter> filterByFieldName = new LinkedHashMap<String, JiraSearchFilter>();

	/** The field the result is ordered by. */
	private String orderBy;

	private boolean ascending = true;

	private int max = -1;

	public JiraSearch(String queryParameter) {
		fromUrl(queryParameter);
	}

	public JiraSearch() {
	}

	public void addFilter(String key, String value) {
		JiraSearchFilter filter = filterByFieldName.get(key);
		if (filter == null) {
			filter = new JiraSearchFilter(key);
			CompareOperator operator = CompareOperator.fromUrl(value);
			filter.setOperator(operator);
			filterByFieldName.put(key, filter);
		}

		filter.addValue(value.substring(filter.getOperator().getQueryValue().length()));
	}

	public void addFilter(JiraSearchFilter filter) {
		filterByFieldName.put(filter.getFieldName(), filter);
	}

	public List<JiraSearchFilter> getFilters() {
		return new ArrayList<JiraSearchFilter>(filterByFieldName.values());
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getOrderBy() {
		return orderBy;
	}

	/**
	 * @see #toQuery(boolean)
	 */
	public String toQuery() {
		return toQuery(false);
	}

	/**
	 * Returns a Jira query string that conforms to the format defined at {@link http
	 * ://projects.edgewall.com/jira/wiki/JiraQuery#QueryLanguage}.
	 * 
	 * @return the empty string, if no search order and criteria are defined; a string that starts with &amp;, otherwise
	 */
	public String toQuery(boolean supportsMax) {
		StringBuilder sb = new StringBuilder();
		if (orderBy != null) {
			sb.append("&order="); //$NON-NLS-1$
			sb.append(orderBy);
			if (!ascending) {
				sb.append("&desc=1"); //$NON-NLS-1$
			}
		}
		if (supportsMax && max != -1) {
			sb.append("&max="); //$NON-NLS-1$
			sb.append(max);
		}
		for (JiraSearchFilter filter : filterByFieldName.values()) {
			sb.append("&"); //$NON-NLS-1$
			sb.append(filter.getFieldName());
			sb.append(filter.getOperator().getQueryValue());
			sb.append("="); //$NON-NLS-1$
			List<String> values = filter.getValues();
			for (Iterator<String> it = values.iterator(); it.hasNext();) {
				sb.append(escapeValue(it.next()));
				if (it.hasNext()) {
					sb.append("|"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	private String escapeValue(String text) {
		text = text.replaceAll("&", "\\\\&"); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replaceAll("\\|", "\\\\|"); //$NON-NLS-1$ //$NON-NLS-2$
		return text;
	}

	/**
	 * Returns a URL encoded string that can be passed as an argument to the Jira query script.
	 * 
	 * @return the empty string, if no search order and criteria are defined; a string that starts with &amp;, otherwise
	 */
	public String toUrl() {
		StringBuilder sb = new StringBuilder();
		if (orderBy != null) {
			sb.append("&order="); //$NON-NLS-1$
			sb.append(orderBy);
			if (!ascending) {
				sb.append("&desc=1"); //$NON-NLS-1$
			}
		} else if (filterByFieldName.isEmpty()) {
			// TODO figure out why search must be ordered when logged in (otherwise
			// no results will be returned)
			sb.append("&order=id"); //$NON-NLS-1$
		}
		if (max != -1) {
			sb.append("&max="); //$NON-NLS-1$
			sb.append(max);
		}

		for (JiraSearchFilter filter : filterByFieldName.values()) {
			for (String value : filter.getValues()) {
				sb.append("&"); //$NON-NLS-1$
				sb.append(filter.getFieldName());
				sb.append("="); //$NON-NLS-1$
				try {
					sb.append(URLEncoder.encode(filter.getOperator().getQueryValue(), IJiraClient.CHARSET));
					sb.append(URLEncoder.encode(value, IJiraClient.CHARSET));
				} catch (UnsupportedEncodingException e) {
					StatusHandler.log(new Status(IStatus.WARNING, JiraCorePlugin.ID_PLUGIN,
							"Unexpected exception while decoding URL", e)); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	public void fromUrl(String url) {
		StringTokenizer t = new StringTokenizer(url, "&"); //$NON-NLS-1$
		while (t.hasMoreTokens()) {
			String token = t.nextToken();
			int i = token.indexOf("="); //$NON-NLS-1$
			if (i != -1) {
				try {
					String key = URLDecoder.decode(token.substring(0, i), IJiraClient.CHARSET);
					String value = URLDecoder.decode(token.substring(i + 1), IJiraClient.CHARSET);

					if ("order".equals(key)) { //$NON-NLS-1$
						setOrderBy(value);
					} else if ("desc".equals(key)) { //$NON-NLS-1$
						setAscending(!"1".equals(value)); //$NON-NLS-1$
					} else if ("group".equals(key) || "groupdesc".equals(key) || "verbose".equals(key)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						// ignore these parameters
					} else if ("max".equals(key)) { //$NON-NLS-1$
						try {
							setMax(Integer.parseInt(value));
						} catch (NumberFormatException e) {
							StatusHandler.log(new Status(IStatus.WARNING, JiraCorePlugin.ID_PLUGIN, NLS.bind(
									"Illegal format in URL, expected a number ''{0}''", value), e)); //$NON-NLS-1$							
						}
					} else {
						addFilter(key, value);
					}
				} catch (UnsupportedEncodingException e) {
					StatusHandler.log(new Status(IStatus.WARNING, JiraCorePlugin.ID_PLUGIN,
							"Unexpected exception while decoding URL", e)); //$NON-NLS-1$
				}
			}
		}
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

}
