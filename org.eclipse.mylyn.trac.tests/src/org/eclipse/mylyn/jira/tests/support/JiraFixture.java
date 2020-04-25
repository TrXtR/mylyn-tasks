/*******************************************************************************
 * Copyright (c) 2009, 2010 Steffen Pingel and others.
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

package org.eclipse.mylyn.jira.tests.support;

import java.net.Proxy;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.commons.net.IProxyProvider;
import org.eclipse.mylyn.commons.net.WebLocation;
import org.eclipse.mylyn.commons.net.WebUtil;
import org.eclipse.mylyn.commons.repositories.core.auth.UserCredentials;
import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil;
import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil.PrivilegeLevel;
import org.eclipse.mylyn.commons.sdk.util.FixtureConfiguration;
import org.eclipse.mylyn.commons.sdk.util.TestConfiguration;
import org.eclipse.mylyn.internal.jira.core.JiraClientFactory;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.core.JiraRepositoryConnector;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tests.util.TestFixture;

/**
 * Initializes Jira repositories to a defined state. This is done once per test run, since cleaning and initializing the
 * repository for each test method would take too long.
 *
 * @author Steffen Pingel
 */
public class JiraFixture extends TestFixture {

	public static String TAG_MISC = "misc";

	public static final String TAG_TEST = "test";

	private static JiraFixture current;

	public static JiraFixture current() {
		if (current == null) {
			current = TestConfiguration.getDefault().discoverDefault(JiraFixture.class, "jira");
			current.activate();
		}
		return current;
	}

	private final Version accessMode;

	private final String version;

	private final Set<String> tags;

	private final boolean excluded;

	public JiraFixture(Version accessMode, String url, String version, String info) {
		super(JiraCorePlugin.CONNECTOR_KIND, url);
		Assert.isNotNull(accessMode);
		Assert.isNotNull(info);
		this.accessMode = accessMode;
		this.version = version;
		this.tags = new HashSet<String>();
		this.excluded = info.startsWith("Test");
		setInfo("Jira", version, info);
	}

	public JiraFixture(FixtureConfiguration configuration) {
		this(Version.fromVersion(configuration.getProperties().get("version")), configuration.getUrl(),
				configuration.getVersion(), configuration.getInfo());
		if (configuration.getTags() != null) {
			this.tags.addAll(configuration.getTags());
		}
	}

	@Override
	public JiraFixture activate() {
		current = this;
		setUpFramework();
		return this;
	}

	@Override
	protected TestFixture getDefault() {
		return TestConfiguration.getDefault().discoverDefault(JiraFixture.class, "jira");
	}

	public IJiraClient connect() throws Exception {
		return connect(repositoryUrl);
	}

	public IJiraClient connectXmlRpc(PrivilegeLevel level) throws Exception {
		UserCredentials credentials = CommonTestUtil.getCredentials(level);
		return connect(repositoryUrl, credentials.getUserName(), credentials.getPassword(),
				getDefaultProxy(repositoryUrl), Version.XML_RPC);
	}

	private Proxy getDefaultProxy(String url) {
		return WebUtil.getProxyForUrl(url);
	}

	public IJiraClient connect(String url) throws Exception {
		return connect(url, getDefaultProxy(url), PrivilegeLevel.USER);
	}

	public IJiraClient connect(String url, Proxy proxy, PrivilegeLevel level) throws Exception {
		UserCredentials credentials = CommonTestUtil.getCredentials(level);
		return connect(url, credentials.getUserName(), credentials.getPassword(), proxy);
	}

	public IJiraClient connect(String url, String username, String password) throws Exception {
		return connect(url, username, password, getDefaultProxy(url));
	}

	public IJiraClient connect(String url, String username, String password, Proxy proxy) throws Exception {
		return connect(url, username, password, proxy, accessMode);
	}

	public IJiraClient connect(String url, String username, String password, final Proxy proxy, Version version)
			throws Exception {
		WebLocation location = new WebLocation(url, username, password, new IProxyProvider() {
			public Proxy getProxyForHost(String host, String proxyType) {
				return proxy;
			}
		});
		return JiraClientFactory.createClient(location, version);
	}

	public Version getAccessMode() {
		return accessMode;
	}

	public String getVersion() {
		return version;
	}

	public boolean isXmlRpcEnabled() {
		return Version.XML_RPC.name().equals(getAccessMode());
	}

	public TaskRepository singleRepository(JiraRepositoryConnector connector) {
		connector.getClientManager().writeCache();
		TaskRepository repository = super.singleRepository();

		// XXX avoid failing test due to stale client
		connector.getClientManager().clearClients();

		connector.getClientManager().readCache();
		return repository;
	}

	@Override
	public TaskRepository singleRepository() {
		return singleRepository(connector());
	}

	@Override
	protected void configureRepository(TaskRepository repository) {
		repository.setTimeZoneId(IJiraClient.TIME_ZONE);
		repository.setCharacterEncoding(IJiraClient.CHARSET);
		repository.setVersion(accessMode.name());
	}

	@Override
	protected void resetRepositories() {
		JiraCorePlugin.getDefault().getConnector().getClientManager().clearClients();
	}

	@Override
	public JiraRepositoryConnector connector() {
		return (JiraRepositoryConnector) super.connector();
	}

	public JiraHarness createHarness() {
		return new JiraHarness(this);
	}

	@Override
	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	public boolean requiresAuthentication() {
		return getInfo().contains("AllBasicAuth");
	}

	@Override
	public boolean isExcluded() {
		return super.isExcluded() || excluded;
	}

	public void waitToGuaranteeTaskUpdate() {
		if (getVersion().compareTo("0.12") < 0) {
			// Jira 0.11 can fail with database errors if subsequent task updates happen too quickly
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
