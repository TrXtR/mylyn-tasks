/*******************************************************************************
 * Copyright (c) 2011 Tasktop Technologies.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.jira.core.client;

/**
 * @author Steffen Pingel
 */
public class JiraSslCertificateException extends JiraException {

	private static final long serialVersionUID = -693879319319751584L;

	public JiraSslCertificateException() {
		super("Opening of the certificate keystore failed"); //$NON-NLS-1$
	}

}
