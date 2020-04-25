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
 *     David Green - improvements
 *     Jan Mauersberger - fixes for bug 350931
 *******************************************************************************/

package org.eclipse.mylyn.jira.tests.ui;

import java.util.List;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.jira.ui.JiraHyperlinkUtil;
import org.eclipse.mylyn.internal.jira.ui.WebHyperlink;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TaskHyperlink;

import junit.framework.TestCase;

/**
 * @author Steffen Pingel
 * @author David Green
 * @see http://jira.edgewall.org/wiki/JiraLinks
 */
public class JiraHyperlinkUtilTest extends TestCase {

	private TaskRepository repository;

	@Override
	protected void setUp() throws Exception {
		repository = new TaskRepository(JiraCorePlugin.CONNECTOR_KIND, "http://localhost");
	}

	public void testFindHyperlinksComment() {
		IHyperlink[] links = findJiraHyperlinks(repository, "comment:ticket:12:34", 0, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 20), links[0].getHyperlinkRegion());
		assertEquals("12", ((TaskHyperlink) links[0]).getTaskId());
	}

	public void testFindHyperlinksTicket() {
		IHyperlink[] links = JiraHyperlinkUtil.findTicketHyperlinks(repository, "#11", 0, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 3), links[0].getHyperlinkRegion());
		assertEquals("11", ((TaskHyperlink) links[0]).getTaskId());

		links = JiraHyperlinkUtil.findTicketHyperlinks(repository, "#11, #234", 6, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("234", ((TaskHyperlink) links[0]).getTaskId());

		links = JiraHyperlinkUtil.findTicketHyperlinks(repository, "  ticket:123  ", 2, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals(new Region(2, 10), links[0].getHyperlinkRegion());
		assertEquals("123", ((TaskHyperlink) links[0]).getTaskId());
	}

	public void testFindHyperlinksNoTicket() {
		IHyperlink[] links = findJiraHyperlinks(repository, "#11", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "#11, #234", 6, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "  ticket:123  ", 2, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "#123 report:123", -1, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/report/123", ((WebHyperlink) links[0]).getURLString());
	}

	public void testFindHyperlinksReport() {
		IHyperlink[] links = findJiraHyperlinks(repository, "report:123", 0, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 10), links[0].getHyperlinkRegion());
		assertEquals("http://localhost/report/123", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "{123}", 0, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 5), links[0].getHyperlinkRegion());
		assertEquals("http://localhost/report/123", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "{{123}}", -1, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(1, 5), links[0].getHyperlinkRegion());

		links = findJiraHyperlinks(repository, "{abc}", -1, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "{{abc}}", -1, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "{{{123}}}", -1, 0);
		assertNull(links);
	}

	public void testFindHyperlinksChangeset() {
		IHyperlink[] links = findJiraHyperlinks(repository, "r123", 0, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 4), links[0].getHyperlinkRegion());
		assertEquals("http://localhost/changeset/123", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "alr123", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "[123]", 0, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 5), links[0].getHyperlinkRegion());
		assertEquals("http://localhost/changeset/123", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "![123]", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "![123]", 1, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "changeset:123", 0, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 13), links[0].getHyperlinkRegion());
		assertEquals("http://localhost/changeset/123", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "[123/trunk]", 0, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 11), links[0].getHyperlinkRegion());
		assertEquals("http://localhost/changeset/123/trunk", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "changeset:123/trunk", 0, 0);
		assertEquals(1, links.length);
		assertEquals(new Region(0, 19), links[0].getHyperlinkRegion());
		assertEquals("http://localhost/changeset/123/trunk", ((WebHyperlink) links[0]).getURLString());
	}

	public void testFindHyperlinksRevisionLog() {
		IHyperlink[] links = findJiraHyperlinks(repository, "r123:456", 0, 0);
		assertEquals(2, links.length);
		assertEquals("http://localhost/log/?rev=123&stop_rev=456", ((WebHyperlink) links[0]).getURLString());
		assertEquals(new Region(0, 8), links[0].getHyperlinkRegion());

		links = findJiraHyperlinks(repository, "[123:456]", 0, 0);
		assertEquals(1, links.length);
		assertEquals("http://localhost/log/?rev=123&stop_rev=456", ((WebHyperlink) links[0]).getURLString());
		assertEquals(new Region(0, 9), links[0].getHyperlinkRegion());

		links = findJiraHyperlinks(repository, "log:@123:456", 0, 0);
		assertEquals(1, links.length);
		assertEquals("http://localhost/log/?rev=123&stop_rev=456", ((WebHyperlink) links[0]).getURLString());
		assertEquals(new Region(0, 12), links[0].getHyperlinkRegion());

		links = findJiraHyperlinks(repository, "log:trunk@123:456", 0, 0);
		assertEquals(1, links.length);
		assertEquals("http://localhost/log/trunk?rev=123&stop_rev=456", ((WebHyperlink) links[0]).getURLString());
		assertEquals(new Region(0, 17), links[0].getHyperlinkRegion());
	}

	public void testFindHyperlinksDiff() {
		IHyperlink[] links = findJiraHyperlinks(repository, "diff:@123:456", 0, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/changeset/?new=456&old=123", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "diff:trunk/jira@3538//sandbox/vc-refactoring/jira@3539", 0, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals(
				"http://localhost/changeset/?new_path=sandbox%2Fvc-refactoring%2Fjira&old_path=trunk%2Fjira&new=3539&old=3538",
				((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "diff:tags/jira-0.9.2/wiki-default//tags/jira-0.9.3/wiki-default", 0, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals(
				"http://localhost/changeset/?new_path=tags%2Fjira-0.9.3%2Fwiki-default&old_path=tags%2Fjira-0.9.2%2Fwiki-default",
				((WebHyperlink) links[0]).getURLString());
	}

	public void testFindHyperlinksWiki() {
		IHyperlink[] links = findJiraHyperlinks(repository, "[wiki:page]", 1, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/wiki/page", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "wiki:page", 0, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/wiki/page", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "Page", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "!Page", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "ab Page dc", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "paGe", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "WikiPage", 0, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/wiki/WikiPage", ((WebHyperlink) links[0]).getURLString());
		assertEquals(new Region(0, 8), links[0].getHyperlinkRegion());

		links = findJiraHyperlinks(repository, "!WikiPage", 0, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "!WikiPage", 1, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "a WikiPage is here", 4, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/wiki/WikiPage", ((WebHyperlink) links[0]).getURLString());
		assertEquals(new Region(2, 8), links[0].getHyperlinkRegion());

		links = findJiraHyperlinks(repository, "a !WikiPage is here", 4, 0);
		assertNull(links);
	}

	public void testFindHyperlinksWikiTwoCamelCaseWork() {
		IHyperlink[] links = findJiraHyperlinks(repository, "aWIkiPage is here", 2, 0);
		assertNull(links);

		links = findJiraHyperlinks(repository, "aWIkiPage is here", 4, 0);
		assertNull(links);
	}

	public void testFindHyperlinksMilestone() {
		IHyperlink[] links = findJiraHyperlinks(repository, "milestone:1.0", 1, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/milestone/1.0", ((WebHyperlink) links[0]).getURLString());
	}

	public void testFindHyperlinksAttachment() {
		IHyperlink[] links = findJiraHyperlinks(repository, "attachment:ticket:123:foo.bar", 1, 0);
		assertNotNull(links);
		assertEquals("123", ((TaskHyperlink) links[0]).getTaskId());
	}

	public void testFindHyperlinksFiles() {
		IHyperlink[] links = findJiraHyperlinks(repository, "source:trunk/foo", 1, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/browser/trunk/foo", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "source:trunk/foo@123", 1, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/browser/trunk/foo?rev=123", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "source:trunk/foo@123#L456", 1, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/browser/trunk/foo?rev=123#L456", ((WebHyperlink) links[0]).getURLString());

		links = findJiraHyperlinks(repository, "source:/tags/foo_bar-1.1", 1, 0);
		assertNotNull(links);
		assertEquals(1, links.length);
		assertEquals("http://localhost/browser/tags/foo_bar-1.1", ((WebHyperlink) links[0]).getURLString());
	}

	private IHyperlink[] findJiraHyperlinks(TaskRepository repository, String text, int offsetInText, int textOffset) {
		List<IHyperlink> links = JiraHyperlinkUtil.findJiraHyperlinks(repository, text, offsetInText, textOffset);
		return (links.isEmpty()) ? null : links.toArray(new IHyperlink[0]);
	}

}
