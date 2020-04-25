/*******************************************************************************
 * Copyright (c) 2006, 2008 Steffen Pingel and others.
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearchFilter;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearchFilter.CompareOperator;
import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.internal.jira.ui.JiraUiPlugin;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Steffen Pingel
 */
public class JiraFilterQueryPage extends AbstractRepositoryQueryPage {

	private static final String TITLE = Messages.JiraFilterQueryPage_New_Jira_Query;

	private static final String DESCRIPTION = Messages.JiraFilterQueryPage_Add_search_filters_to_define_query;

	private static final String TITLE_QUERY_TITLE = Messages.JiraFilterQueryPage_Query_Title;

	private Text titleText;

	private Composite scrollComposite;

	/* Maintain order of criterions in order to be able to restore this later. */
	private final Set<SearchField> visibleSearchFields = new LinkedHashSet<SearchField>();

	private List<SearchField> searchFields;

	public JiraFilterQueryPage(TaskRepository repository, IRepositoryQuery query) {
		super(TITLE, repository, query);
		setTitle(TITLE);
		setDescription(DESCRIPTION);
	}

	@Override
	public void applyTo(IRepositoryQuery query) {
		query.setUrl(getQueryUrl(getTaskRepository().getRepositoryUrl()));
		query.setSummary(getQueryTitle());
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData());
		composite.setLayout(new GridLayout(1, false));

		createTitleGroup(composite);

		ScrolledComposite scrolledComposite = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, true, true);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setLayoutData(gd);

		scrollComposite = new Composite(scrolledComposite, SWT.None);
		scrolledComposite.setContent(scrollComposite);
		scrollComposite.setLayout(new GridLayout(4, false));

		createAddFilterGroup(composite);

		if (getQuery() != null) {
			titleText.setText(getQuery().getSummary());
			JiraSearch search = JiraUtil.toJiraSearch(getQuery());
			if (search != null) {
				restoreWidgetValues(search);
			}
		}

		Dialog.applyDialogFont(composite);
		setControl(composite);
	}

	@Override
	public String getQueryTitle() {
		return (titleText != null) ? titleText.getText() : null;
	}

	private void restoreWidgetValues(JiraSearch search) {
		List<JiraSearchFilter> filters = search.getFilters();
		for (JiraSearchFilter filter : filters) {
			SearchField field = getSearchField(filter.getFieldName());
			if (field != null) {
				showSearchField(field, filter);
			} else {
				StatusHandler.log(new Status(IStatus.WARNING, JiraUiPlugin.ID_PLUGIN,
						"Ignoring invalid search filter: " + filter)); //$NON-NLS-1$
			}
		}
	}

	private SearchField getSearchField(String fieldName) {
		for (SearchField searchField : searchFields) {
			if (searchField.getFieldName().equals(fieldName)) {
				return searchField;
			}
		}
		return null;
	}

	private void createAddFilterGroup(Composite parent) {
		GridLayout layout;
		GridData gd;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout(2, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		composite.setLayoutData(gd);

		Label label = new Label(composite, SWT.LEFT);
		label.setText(Messages.JiraFilterQueryPage_Select_to_add_filter);

		// condition
		final Combo filterCombo = new Combo(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);

		searchFields = new ArrayList<SearchField>();
		searchFields.add(new TextSearchField("summary", Messages.JiraFilterQueryPage_Summary)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("reporter", Messages.JiraFilterQueryPage_Reporter)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("owner", Messages.JiraFilterQueryPage_Owner)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("type", Messages.JiraFilterQueryPage_Type)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("status", Messages.JiraFilterQueryPage_Status)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("priority", Messages.JiraFilterQueryPage_Priority)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("milestone", Messages.JiraFilterQueryPage_Milestone)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("component", Messages.JiraFilterQueryPage_Component)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("version", Messages.JiraFilterQueryPage_Version)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("resoution", Messages.JiraFilterQueryPage_Resolution)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("keywords", Messages.JiraFilterQueryPage_Keywords)); //$NON-NLS-1$
		searchFields.add(new TextSearchField("cc", Messages.JiraFilterQueryPage_CC)); //$NON-NLS-1$

		filterCombo.add(""); //$NON-NLS-1$
		for (SearchField field : searchFields) {
			filterCombo.add(field.getDisplayName());
		}

		filterCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (filterCombo.getText().length() > 0) {
					SearchField field = searchFields.get(filterCombo.getSelectionIndex() - 1);
					showSearchField(field, null);
					filterCombo.setText(""); //$NON-NLS-1$
				}
			}
		});
	}

	private void createTitleGroup(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(TITLE_QUERY_TITLE);
		group.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);

		titleText = new Text(group, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		titleText.setLayoutData(gd);

		titleText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				// ignore
			}

			public void keyReleased(KeyEvent e) {
				getContainer().updateButtons();
			}
		});
	}

	@Override
	public boolean isPageComplete() {
		if (titleText != null && titleText.getText().length() > 0) {
			return true;
		}
		return false;
	}

	private void showSearchField(SearchField field, JiraSearchFilter filter) {
		assert filter == null || !visibleSearchFields.contains(field);

		if (!visibleSearchFields.contains(field)) {
			field.createControls(scrollComposite, filter);
			visibleSearchFields.add(field);
		} else {
			field.addControl(scrollComposite);
		}
		updateScrollPane();
	}

	public String getQueryUrl(String repsitoryUrl) {
		JiraSearch search = new JiraSearch();
		for (SearchField field : visibleSearchFields) {
			search.addFilter(field.getFilter());
		}

		StringBuilder sb = new StringBuilder();
		sb.append(repsitoryUrl);
		sb.append(IJiraClient.QUERY_URL);
		sb.append(search.toUrl());
		return sb.toString();
	}

	private void hideSearchField(SearchField field) {
		visibleSearchFields.remove(field);
	}

	private void updateScrollPane() {
		scrollComposite.setSize(scrollComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrollComposite.layout();
	}

	private abstract class SearchField {

		protected String fieldName;

		private final String displayName;

		public SearchField(String fieldName, String displayName) {
			this.fieldName = fieldName;
			this.displayName = displayName;
		}

		public abstract void createControls(Composite parent, JiraSearchFilter filter);

		public abstract void addControl(Composite parent);

		public String getFieldName() {
			return fieldName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public abstract JiraSearchFilter getFilter();

	}

	private class TextSearchField extends SearchField {

		private final CompareOperator[] compareOperators = { CompareOperator.CONTAINS, CompareOperator.CONTAINS_NOT,
				CompareOperator.BEGINS_WITH, CompareOperator.ENDS_WITH, CompareOperator.IS, CompareOperator.IS_NOT, };

		private List<TextCriterion> criterions;

		public TextSearchField(String fieldName, String displayName) {
			super(fieldName, displayName);
		}

		@Override
		public void createControls(Composite parent, JiraSearchFilter filter) {
			if (filter != null) {
				TextCriterion first = addCriterion(parent);
				first.setCondition(filter.getOperator());
				List<String> values = filter.getValues();
				if (!values.isEmpty()) {
					first.setSearchText(values.get(0));
					for (int i = 1; i < values.size(); i++) {
						TextCriterion criterion = addCriterion(parent);
						criterion.setSearchText(values.get(1));
					}
				}
			} else {
				addCriterion(parent);
			}

		}

		@Override
		public void addControl(Composite parent) {
			addCriterion(parent);
		}

		public TextCriterion addCriterion(Composite parent) {
			TextCriterion criterion = new TextCriterion();
			if (criterions == null) {
				criterions = new ArrayList<TextCriterion>();
				criterion.createControl(parent);
			} else {
				criterion.createControl(parent, criterions.get(criterions.size() - 1));
			}
			criterions.add(criterion);
			return criterion;
		}

		@Override
		public JiraSearchFilter getFilter() {
			JiraSearchFilter newFilter = new JiraSearchFilter(getFieldName());
			newFilter.setOperator(criterions.get(0).getCondition());
			for (TextCriterion criterion : criterions) {
				newFilter.addValue(criterion.getSearchText());
			}
			return newFilter;
		}

		public void removeCriterion(TextCriterion criterion) {
			int i = criterions.indexOf(criterion);
			if (i == -1) {
				throw new RuntimeException();
			}
			if (i == 0) {
				// the first criterion is special since it contains the compare
				// operator combo
				if (criterions.size() > 1) {
					// copy the value from the second criterion to the first
					TextCriterion sourceCriterion = criterions.get(1);
					criterion.searchText.setText(sourceCriterion.searchText.getText());
					removeCriterionByIndex(1);
				} else {
					// no more criterions, remove all controls
					removeCriterionByIndex(0);
					hideSearchField(this);
				}
			} else {
				removeCriterionByIndex(i);
			}
		}

		private void removeCriterionByIndex(int i) {
			criterions.get(i).remove();
			criterions.remove(i);
			updateScrollPane();
		}

		private class TextCriterion {

			private Combo conditionCombo;

			private Text searchText;

			private Label label;

			private Button removeButton;

			public void createControl(Composite parent) {
				label = new Label(parent, SWT.LEFT);
				label.setText(getDisplayName() + ": "); //$NON-NLS-1$

				conditionCombo = new Combo(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
				for (CompareOperator op : compareOperators) {
					conditionCombo.add(op.toString());
				}
				conditionCombo.setText(compareOperators[0].toString());

				createSearchTextAndRemoveButton(parent);
			}

			public void createControl(Composite parent, TextCriterion top) {
				label = new Label(parent, SWT.RIGHT);
				GridData gd = new GridData();
				gd.horizontalAlignment = SWT.END;
				gd.horizontalSpan = 2;
				label.setLayoutData(gd);
				label.setText(Messages.JiraFilterQueryPage_or);

				createSearchTextAndRemoveButton(parent);

				label.moveBelow(top.removeButton);
				searchText.moveBelow(label);
				removeButton.moveBelow(searchText);
			}

			private void createSearchTextAndRemoveButton(Composite parent) {
				searchText = new Text(parent, SWT.BORDER);
				GridData gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
				searchText.setLayoutData(gd);

				removeButton = new Button(parent, SWT.PUSH);
				removeButton.setText("-"); //$NON-NLS-1$
				removeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						TextSearchField.this.removeCriterion(TextCriterion.this);
					}
				});
			}

			public void remove() {
				label.dispose();
				if (conditionCombo != null) {
					conditionCombo.dispose();
				}
				searchText.dispose();
				removeButton.dispose();
			}

			public CompareOperator getCondition() {
				return (conditionCombo != null) ? compareOperators[conditionCombo.getSelectionIndex()] : null;
			}

			public String getSearchText() {
				return searchText.getText();
			}

			public boolean setCondition(CompareOperator operator) {
				if (conditionCombo != null) {
					int i = conditionCombo.indexOf(operator.toString());
					if (i != -1) {
						conditionCombo.select(i);
						return true;
					}
				}
				return false;
			}

			public void setSearchText(String text) {
				searchText.setText(text);
			}

		}
	}

}
