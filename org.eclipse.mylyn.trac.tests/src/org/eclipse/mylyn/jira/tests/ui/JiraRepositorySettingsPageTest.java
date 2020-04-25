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

package org.eclipse.mylyn.jira.tests.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.jira.core.JiraRepositoryConnector;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient.Version;
import org.eclipse.mylyn.internal.jira.ui.wizard.JiraRepositorySettingsPage;
import org.eclipse.mylyn.internal.jira.ui.wizard.JiraRepositorySettingsPage.JiraValidator;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.wizards.EditRepositoryWizard;
import org.eclipse.mylyn.jira.tests.support.JiraFixture;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tests.util.TasksUiTestUtil;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import junit.framework.TestCase;

/**
 * @author Steffen Pingel
 */
public class JiraRepositorySettingsPageTest extends TestCase {

	// make protected methods visible
	private static class MyJiraRepositorySettingsPage extends JiraRepositorySettingsPage {

		public MyJiraRepositorySettingsPage(TaskRepository taskRepository) {
			super(taskRepository);
		}

		@Override
		protected void applyValidatorResult(Validator validator) {
			// see AbstractRespositorySettingsPage.validate()
			if (validator.getStatus() == null) {
				validator.setStatus(Status.OK_STATUS);
			}
			super.applyValidatorResult(validator);
		}

		@Override
		protected boolean isValidUrl(String name) {
			return super.isValidUrl(name);
		}

	}

	private MyJiraRepositorySettingsPage page;

	private JiraValidator validator;

	private WizardDialog dialog;

	private JiraFixture fixture;

	public JiraRepositorySettingsPageTest() {
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		page = new MyJiraRepositorySettingsPage(null);

		// stub wizard and dialog
		Wizard wizard = new Wizard() {
			@Override
			public boolean performFinish() {
				return true;
			}
		};
		wizard.addPage(page);
		dialog = new WizardDialog(null, wizard);
		dialog.create();
//		page.createControl(dialog.getShell());
//		page.setVisible(true);

		fixture = JiraFixture.current();
	}

	@Override
	protected void tearDown() throws Exception {
		if (dialog != null) {
			dialog.close();
		}
	}

	protected void initialize(JiraFixture fixture) throws Exception {
		// initialize page from test fixture
		TaskRepository repository = fixture.repository();
		page.setAnonymous(false);
		page.setUrl(repository.getRepositoryUrl());
		AuthenticationCredentials credentials = repository.getCredentials(AuthenticationType.REPOSITORY);
		page.setUserId(credentials == null ? null : credentials.getUserName());
		page.setPassword(credentials == null ? null : credentials.getPassword());
		page.setJiraVersion(fixture.getAccessMode());

		validator = page.new JiraValidator(page.createTaskRepository(), fixture.getAccessMode());
	}

	public void testValidate() throws Exception {
		initialize(fixture);

		validator.run(new NullProgressMonitor());
		assertNull(validator.getResult());
		assertNull(validator.getStatus());

		page.applyValidatorResult(validator);
		assertEquals(fixture.getAccessMode(), page.getJiraVersion());
		assertEquals("Authentication credentials are valid.", page.getMessage());
	}

	public void testValidateAutomaticUser() throws Exception {
		initialize(fixture);

		page.setJiraVersion(null);
		validator = page.new JiraValidator(page.createTaskRepository(), null);

		validator.run(new NullProgressMonitor());
		assertEquals(Version.XML_RPC, validator.getResult());
		assertNull(validator.getStatus());

		page.applyValidatorResult(validator);
		assertEquals(Version.XML_RPC, page.getJiraVersion());
		assertEquals("Authentication credentials are valid.", page.getMessage());
	}

	public void testValidateAutomaticAnonymous() throws Exception {
		initialize(fixture);

		page.setUserId("");
		page.setPassword("");
		page.setJiraVersion(null);
		validator = page.new JiraValidator(page.createTaskRepository(), null);

		validator.run(new NullProgressMonitor());
		assertEquals(Version.JIRA_0_9, validator.getResult());
		assertNotNull(validator.getStatus());

		page.applyValidatorResult(validator);
		assertEquals(Version.JIRA_0_9, page.getJiraVersion());
		assertEquals(
				"Authentication credentials are valid. Note: Insufficient permissions for XML-RPC access, falling back to web access.",
				page.getMessage());
	}

	public void testValidateInvalid() throws Exception {
		initialize(fixture);

		page.setUrl("http://mylyn.org/doesnotexist");
		page.setJiraVersion(null);
		validator = page.new JiraValidator(page.createTaskRepository(), null);

		try {
			validator.run(new NullProgressMonitor());
			fail("Expected CoreException");
		} catch (CoreException e) {
			validator.setStatus(e.getStatus());
		}

		page.applyValidatorResult(validator);
		assertNull(page.getJiraVersion());
		assertEquals(IMessageProvider.ERROR, page.getMessageType());
	}

	public void testValidUrl() throws Exception {
		assertFalse(page.isValidUrl(""));
		assertFalse(page.isValidUrl("http:/google.com"));
		assertFalse(page.isValidUrl("http:/google.com/"));
		assertFalse(page.isValidUrl("http://google.com/"));
		assertFalse(page.isValidUrl("http://google.com/foo /space"));

		assertTrue(page.isValidUrl("http://google.com"));
		assertTrue(page.isValidUrl("https://google.com"));
		assertTrue(page.isValidUrl("http://mylyn.org/jira30"));
		assertTrue(page.isValidUrl("http://www.mylyn.org/jira30"));
	}

	public void testClientManagerChangeTaskRepositorySettings() throws Exception {
		JiraRepositoryConnector connector = fixture.connector();
		TaskRepository repository = fixture.singleRepository();
		repository.setVersion(Version.JIRA_0_9.name());
		IJiraClient client = connector.getClientManager().getJiraClient(repository);
		assertEquals(Version.JIRA_0_9, client.getAccessMode());

		TasksUiTestUtil.ensureTasksUiInitialization();

		EditRepositoryWizard wizard = new EditRepositoryWizard(repository,
				TasksUiPlugin.getConnectorUi(repository.getConnectorKind()));
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		WizardDialog dialog = new WizardDialog(shell, wizard);
		try {
			dialog.create();

			((JiraRepositorySettingsPage) wizard.getSettingsPage()).setJiraVersion(Version.XML_RPC);
			assertTrue(wizard.performFinish());

			client = connector.getClientManager().getJiraClient(repository);
			assertEquals(Version.XML_RPC, client.getAccessMode());
		} finally {
			dialog.close();
		}
	}

}
