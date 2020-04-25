/*******************************************************************************
 * Copyright (c) 2009 Steffen Pingel and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steffen Pingel
 */
public class JiraAction {

	private List<JiraTicketField> fields;

	private String hint;

	private String id;

	private String label;

	public JiraAction(String id) {
		this.id = id;
	}

	public void addField(JiraTicketField field) {
		if (fields == null) {
			fields = new ArrayList<JiraTicketField>();
		}
		fields.add(field);
	}

	public List<JiraTicketField> getFields() {
		if (fields == null) {
			return Collections.emptyList();
		}
		return new ArrayList<JiraTicketField>(fields);
	}

	public String getHint() {
		return hint;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public void removeField(JiraTicketField field) {
		if (fields != null) {
			fields.remove(field);
		}
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
