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
 *     Benjamin Muskalla (Tasktop Technologies) - support for deleting tasks
 *******************************************************************************/

package org.eclipse.mylyn.internal.jira.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.internal.jira.core.client.AbstractWikiHandler;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient;
import org.eclipse.mylyn.internal.jira.core.client.IJiraWikiClient;
import org.eclipse.mylyn.internal.jira.core.client.JiraException;
import org.eclipse.mylyn.internal.jira.core.client.IJiraClient.Version;
import org.eclipse.mylyn.internal.jira.core.model.JiraTicket;
import org.eclipse.mylyn.internal.jira.core.model.JiraComment;
import org.eclipse.mylyn.internal.jira.core.model.JiraPriority;
import org.eclipse.mylyn.internal.jira.core.model.JiraSearch;
import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskHistory;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskRelation;
import org.eclipse.mylyn.tasks.core.data.TaskRevision;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

/**
 * @author Steffen Pingel
 * @author Benjamin Muskalla
 */
public class JiraRepositoryConnector extends AbstractRepositoryConnector {

	public enum TaskKind {
		DEFECT, ENHANCEMENT, TASK, STORY;

		public static TaskKind fromString(String type) {
			if (type == null) {
				return null;
			}
			if (type.equals("Defect")) { //$NON-NLS-1$
				return DEFECT;
			}
			if (type.equals("Enhancement")) { //$NON-NLS-1$
				return ENHANCEMENT;
			}
			if (type.equals("Task")) { //$NON-NLS-1$
				return TASK;
			}
			if (type.equals("Story")) { //$NON-NLS-1$
				return STORY;
			}
			return null;
		}

		public static TaskKind fromType(String type) {
			if (type == null) {
				return null;
			}
			if (type.equals("defect") || type.equals("error")) { //$NON-NLS-1$ //$NON-NLS-2$
				return DEFECT;
			}
			if (type.equals("enhancement")) { //$NON-NLS-1$
				return ENHANCEMENT;
			}
			if (type.equals("task")) { //$NON-NLS-1$
				return TASK;
			}
			if (type.equals("story")) { //$NON-NLS-1$
				return STORY;
			}
			return null;
		}

		@Override
		public String toString() {
			switch (this) {
			case DEFECT:
				return "Defect"; //$NON-NLS-1$
			case ENHANCEMENT:
				return "Enhancement"; //$NON-NLS-1$
			case TASK:
				return "Task"; //$NON-NLS-1$
			case STORY:
				return "Story"; //$NON-NLS-1$
			default:
				return ""; //$NON-NLS-1$
			}
		}

	}

	public enum TaskStatus {
		ASSIGNED, CLOSED, NEW, REOPENED;

		public static TaskStatus fromStatus(String status) {
			if (status == null) {
				return null;
			}
			if (status.equals("new")) { //$NON-NLS-1$
				return NEW;
			}
			if (status.equals("assigned")) { //$NON-NLS-1$
				return ASSIGNED;
			}
			if (status.equals("reopened")) { //$NON-NLS-1$
				return REOPENED;
			}
			if (status.equals("closed")) { //$NON-NLS-1$
				return CLOSED;
			}
			return null;
		}

		public String toStatusString() {
			switch (this) {
			case NEW:
				return "new"; //$NON-NLS-1$
			case ASSIGNED:
				return "assigned"; //$NON-NLS-1$
			case REOPENED:
				return "reopened"; //$NON-NLS-1$
			case CLOSED:
				return "closed"; //$NON-NLS-1$
			default:
				return ""; //$NON-NLS-1$
			}
		}

		@Override
		public String toString() {
			switch (this) {
			case NEW:
				return "New"; //$NON-NLS-1$
			case ASSIGNED:
				return "Assigned"; //$NON-NLS-1$
			case REOPENED:
				return "Reopened"; //$NON-NLS-1$
			case CLOSED:
				return "Closed"; //$NON-NLS-1$
			default:
				return ""; //$NON-NLS-1$
			}
		}

	}

	public enum JiraPriorityLevel {
		BLOCKER, CRITICAL, MAJOR, MINOR, TRIVIAL;

		public static JiraPriorityLevel fromPriority(String priority) {
			if (priority == null) {
				return null;
			}
			if (priority.equals("blocker")) { //$NON-NLS-1$
				return BLOCKER;
			}
			if (priority.equals("critical")) { //$NON-NLS-1$
				return CRITICAL;
			}
			if (priority.equals("major")) { //$NON-NLS-1$
				return MAJOR;
			}
			if (priority.equals("minor")) { //$NON-NLS-1$
				return MINOR;
			}
			if (priority.equals("trivial")) { //$NON-NLS-1$
				return TRIVIAL;
			}
			return null;
		}

		public PriorityLevel toPriorityLevel() {
			switch (this) {
			case BLOCKER:
				return PriorityLevel.P1;
			case CRITICAL:
				return PriorityLevel.P2;
			case MAJOR:
				return PriorityLevel.P3;
			case MINOR:
				return PriorityLevel.P4;
			case TRIVIAL:
				return PriorityLevel.P5;
			default:
				return null;
			}
		}

		@Override
		public String toString() {
			switch (this) {
			case BLOCKER:
				return "blocker"; //$NON-NLS-1$
			case CRITICAL:
				return "critical"; //$NON-NLS-1$
			case MAJOR:
				return "major"; //$NON-NLS-1$
			case MINOR:
				return "minor"; //$NON-NLS-1$
			case TRIVIAL:
				return "trivial"; //$NON-NLS-1$
			default:
				return null;
			}
		}
	}

	private final static Date DEFAULT_COMPLETION_DATE = new Date(0);

	private static int TASK_PRIORITY_LEVELS = 5;

	public static final String TASK_KEY_SUPPORTS_SUBTASKS = "SupportsSubtasks"; //$NON-NLS-1$

	public static final String TASK_KEY_UPDATE_DATE = "UpdateDate"; //$NON-NLS-1$

	public static String getDisplayUsername(TaskRepository repository) {
		AuthenticationCredentials credentials = repository.getCredentials(AuthenticationType.REPOSITORY);
		if (credentials != null && credentials.getUserName().length() > 0) {
			return IJiraClient.DEFAULT_USERNAME;
		}
		return repository.getUserName();
	}

	public static PriorityLevel getTaskPriority(String jiraPriority) {
		if (jiraPriority != null) {
			JiraPriorityLevel priority = JiraPriorityLevel.fromPriority(jiraPriority);
			if (priority != null) {
				return priority.toPriorityLevel();
			}
		}
		return PriorityLevel.getDefault();
	}

	public static PriorityLevel getTaskPriority(String priority, JiraPriority[] jiraPriorities) {
		if (priority != null && jiraPriorities != null && jiraPriorities.length > 0) {
			int minValue = jiraPriorities[0].getValue();
			int range = jiraPriorities[jiraPriorities.length - 1].getValue() - minValue;
			for (JiraPriority jiraPriority : jiraPriorities) {
				if (priority.equals(jiraPriority.getName())) {
					float relativeValue = (float) (jiraPriority.getValue() - minValue) / range;
					int value = (int) (relativeValue * TASK_PRIORITY_LEVELS) + 1;
					return PriorityLevel.fromLevel(value);
				}
			}
		}
		return getTaskPriority(priority);
	}

	public static int getTicketId(String taskId) throws CoreException {
		try {
			return Integer.parseInt(taskId);
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR, JiraCorePlugin.ID_PLUGIN, IStatus.OK,
					"Invalid ticket id: " + taskId, e)); //$NON-NLS-1$
		}
	}

	static List<String> getAttributeValues(TaskData data, String attributeId) {
		TaskAttribute attribute = data.getRoot().getMappedAttribute(attributeId);
		if (attribute != null) {
			return attribute.getValues();
		} else {
			return Collections.emptyList();
		}
	}

	static String getAttributeValue(TaskData data, String attributeId) {
		TaskAttribute attribute = data.getRoot().getMappedAttribute(attributeId);
		if (attribute != null) {
			return attribute.getValue();
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	public static boolean hasAttachmentSupport(TaskRepository repository, ITask task) {
		return Version.XML_RPC.name().equals(repository.getVersion());
	}

	public static boolean hasChangedSince(TaskRepository repository) {
		return Version.XML_RPC.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository) {
		return Version.XML_RPC.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository, ITask task) {
		return hasRichEditor(repository);
	}

	public static boolean isCompleted(String jiraStatus) {
		TaskStatus taskStatus = TaskStatus.fromStatus(jiraStatus);
		return taskStatus == TaskStatus.CLOSED;
	}

	private final JiraAttachmentHandler attachmentHandler = new JiraAttachmentHandler(this);

	private JiraClientManager clientManager;

	private File repositoryConfigurationCacheFile;

	private final JiraTaskDataHandler taskDataHandler = new JiraTaskDataHandler(this);

	private TaskRepositoryLocationFactory taskRepositoryLocationFactory = new TaskRepositoryLocationFactory();

	private final JiraWikiHandler wikiHandler = new JiraWikiHandler(this);

	public JiraRepositoryConnector() {
		if (JiraCorePlugin.getDefault() != null) {
			JiraCorePlugin.getDefault().setConnector(this);
			IPath path = JiraCorePlugin.getDefault().getRepostioryAttributeCachePath();
			this.repositoryConfigurationCacheFile = path.toFile();
		}
	}

	public JiraRepositoryConnector(File repositoryConfigurationCacheFile) {
		this.repositoryConfigurationCacheFile = repositoryConfigurationCacheFile;
	}

	@Override
	public boolean canCreateNewTask(TaskRepository repository) {
		return true;
	}

	@Override
	public boolean canCreateTaskFromKey(TaskRepository repository) {
		return true;
	}

	@Override
	public boolean canSynchronizeTask(TaskRepository taskRepository, ITask task) {
		return hasRichEditor(taskRepository, task);
	}

	@Override
	public JiraAttachmentHandler getTaskAttachmentHandler() {
		return attachmentHandler;
	}

	public synchronized JiraClientManager getClientManager() {
		if (clientManager == null) {
			clientManager = new JiraClientManager(repositoryConfigurationCacheFile, taskRepositoryLocationFactory);
		}
		return clientManager;
	}

	@Override
	public String getConnectorKind() {
		return JiraCorePlugin.CONNECTOR_KIND;
	}

	@Override
	public String getLabel() {
		return Messages.JiraRepositoryConnector_Jira_Client_Label;
	}

	@Override
	public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor)
			throws CoreException {
		return taskDataHandler.getTaskData(repository, taskId, monitor);
	}

	@Override
	public JiraTaskDataHandler getTaskDataHandler() {
		return taskDataHandler;
	}

	@Override
	public String getRepositoryUrlFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.lastIndexOf(IJiraClient.TICKET_URL);
		return index == -1 ? null : url.substring(0, index);
	}

	@Override
	public String getTaskIdFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.lastIndexOf(IJiraClient.TICKET_URL);
		return index == -1 ? null : url.substring(index + IJiraClient.TICKET_URL.length());
	}

	@Override
	public String getTaskIdPrefix() {
		return "#"; //$NON-NLS-1$
	}

	public TaskRepositoryLocationFactory getTaskRepositoryLocationFactory() {
		return taskRepositoryLocationFactory;
	}

	@Override
	public String getTaskUrl(String repositoryUrl, String taskId) {
		return repositoryUrl + IJiraClient.TICKET_URL + taskId;
	}

	public AbstractWikiHandler getWikiHandler() {
		return wikiHandler;
	}

	public boolean hasWiki(TaskRepository repository) {
		// check the access mode to validate Wiki support
		IJiraClient client = getClientManager().getJiraClient(repository);
		if (client instanceof IJiraWikiClient) {
			return true;
		}
		return false;
	}

	@Override
	public IStatus performQuery(TaskRepository repository, IRepositoryQuery query, TaskDataCollector resultCollector,
			ISynchronizationSession session, IProgressMonitor monitor) {
		try {
			monitor.beginTask(Messages.JiraRepositoryConnector_Querying_repository, IProgressMonitor.UNKNOWN);

			JiraSearch search = JiraUtil.toJiraSearch(query);
			if (search == null) {
				return new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR, JiraCorePlugin.ID_PLUGIN,
						RepositoryStatus.ERROR_REPOSITORY, "The query is invalid: \"" + query.getUrl() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			search.setMax(TaskDataCollector.MAX_HITS);

			IJiraClient client;
			try {
				Map<String, ITask> taskById = null;

				client = getClientManager().getJiraClient(repository);
				client.updateAttributes(monitor, false);

				if (session != null && session.isFullSynchronization() && hasRichEditor(repository)
						&& !session.getTasks().isEmpty()) {
					// performance optimization: only fetch task ids, all changed tasks have already been marked stale by preSynchronization() 
					List<Integer> ticketIds = new ArrayList<Integer>();
					client.searchForTicketIds(search, ticketIds, monitor);

					for (Integer id : ticketIds) {
						if (taskById == null) {
							taskById = new HashMap<String, ITask>();
							for (ITask task : session.getTasks()) {
								taskById.put(task.getTaskId(), task);
							}
						}
						TaskData taskData = new TaskData(taskDataHandler.getAttributeMapper(repository),
								JiraCorePlugin.CONNECTOR_KIND, repository.getRepositoryUrl(), id + ""); //$NON-NLS-1$
						taskData.setPartial(true);
						TaskAttribute attribute = JiraTaskDataHandler.createAttribute(taskData, JiraAttribute.ID);
						attribute.setValue(id + ""); //$NON-NLS-1$
						resultCollector.accept(taskData);
					}
				} else {
					List<JiraTicket> tickets = new ArrayList<JiraTicket>();
					client.search(search, tickets, monitor);

					for (JiraTicket ticket : tickets) {
						TaskData taskData = taskDataHandler.createTaskDataFromTicket(client, repository, ticket,
								monitor);
						taskData.setPartial(true);
						if (session != null && !session.isFullSynchronization() && hasRichEditor(repository)) {
							if (taskById == null) {
								taskById = new HashMap<String, ITask>();
								for (ITask task : session.getTasks()) {
									taskById.put(task.getTaskId(), task);
								}
							}
							// preSyncronization() only handles full synchronizations
							ITask task = taskById.get(ticket.getId() + ""); //$NON-NLS-1$
							if (task != null && hasTaskChanged(repository, task, taskData)) {
								session.markStale(task);
							}
						}
						resultCollector.accept(taskData);
					}
				}
			} catch (OperationCanceledException e) {
				throw e;
			} catch (Throwable e) {
				return JiraCorePlugin.toStatus(e, repository);
			}

			return Status.OK_STATUS;
		} finally {
			monitor.done();
		}
	}

	@Override
	public void postSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			if (event.isFullSynchronization() && event.getStatus() == null) {
				Date date = getSynchronizationTimestamp(event);
				if (date != null) {
					event.getTaskRepository().setSynchronizationTimeStamp(JiraUtil.toJiraTime(date) + ""); //$NON-NLS-1$
				}
			}
		} finally {
			monitor.done();
		}
	}

	private Date getSynchronizationTimestamp(ISynchronizationSession event) {
		Date mostRecent = new Date(0);
		Date mostRecentTimeStamp = JiraUtil.parseDate(event.getTaskRepository().getSynchronizationTimeStamp());
		for (ITask task : event.getChangedTasks()) {
			Date taskModifiedDate = task.getModificationDate();
			if (taskModifiedDate != null && taskModifiedDate.after(mostRecent)) {
				mostRecent = taskModifiedDate;
				mostRecentTimeStamp = task.getModificationDate();
			}
		}
		return mostRecentTimeStamp;
	}

	@Override
	public void preSynchronization(ISynchronizationSession session, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(Messages.JiraRepositoryConnector_Getting_changed_tasks, IProgressMonitor.UNKNOWN);

			if (!session.isFullSynchronization()) {
				return;
			}

			// there are no Jira tasks in the task list, skip contacting the repository
			if (session.getTasks().isEmpty()) {
				return;
			}

			TaskRepository repository = session.getTaskRepository();
			if (!JiraRepositoryConnector.hasChangedSince(repository)) {
				// always run the queries for web mode
				return;
			}

			if (repository.getSynchronizationTimeStamp() == null
					|| repository.getSynchronizationTimeStamp().length() == 0) {
				for (ITask task : session.getTasks()) {
					session.markStale(task);
				}
				return;
			}

			Date since = new Date(0);
			try {
				since = JiraUtil.parseDate(Integer.parseInt(repository.getSynchronizationTimeStamp()));
			} catch (NumberFormatException e) {
			}

			try {
				IJiraClient client = getClientManager().getJiraClient(repository);
				Set<Integer> ids = client.getChangedTickets(since, monitor);
//				if (CoreUtil.TEST_MODE) {
//					System.err.println(" preSynchronization(): since=" + since.getTime() + ",changed=" + ids); //$NON-NLS-1$ //$NON-NLS-2$ 
//				}
				if (ids.isEmpty()) {
					// repository is unchanged
					session.setNeedsPerformQueries(false);
					return;
				}

				if (ids.size() == 1) {
					// getChangedTickets() is expected to always return at least
					// one ticket because
					// the repository synchronization timestamp is set to the
					// most recent modification date
					Integer id = ids.iterator().next();
					Date lastChanged = client.getTicketLastChanged(id, monitor);
//					if (CoreUtil.TEST_MODE) {
//						System.err.println(" preSynchronization(): since=" + since.getTime() + ", lastChanged=" + lastChanged.getTime()); //$NON-NLS-1$ //$NON-NLS-2$
//					}
					if (since.equals(lastChanged)) {
						// repository didn't actually change
						session.setNeedsPerformQueries(false);
						return;
					}
				}

				for (ITask task : session.getTasks()) {
					Integer id = getTicketId(task.getTaskId());
					if (ids.contains(id)) {
						session.markStale(task);
					}
				}
			} catch (OperationCanceledException e) {
				throw e;
			} catch (Exception e) {
				// TODO catch JiraException
				throw new CoreException(JiraCorePlugin.toStatus(e, repository));
			}
		} finally {
			monitor.done();
		}
	}

	public synchronized void setTaskRepositoryLocationFactory(
			TaskRepositoryLocationFactory taskRepositoryLocationFactory) {
		this.taskRepositoryLocationFactory = taskRepositoryLocationFactory;
		if (this.clientManager != null) {
			clientManager.setTaskRepositoryLocationFactory(taskRepositoryLocationFactory);
		}
	}

	public void stop() {
		if (clientManager != null) {
			clientManager.writeCache();
		}
	}

	@Override
	public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
		try {
			IJiraClient client = getClientManager().getJiraClient(repository);
			client.updateAttributes(monitor, true);
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Throwable e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		}
	}

	@Override
	public void updateTaskFromTaskData(TaskRepository taskRepository, ITask task, TaskData taskData) {
		TaskMapper mapper = getTaskMapping(taskData);
		mapper.applyTo(task);
		String status = mapper.getStatus();
		if (status != null) {
			if (isCompleted(mapper.getStatus())) {
				Date modificationDate = mapper.getModificationDate();
				if (modificationDate == null) {
					// web mode does not set a date
					modificationDate = DEFAULT_COMPLETION_DATE;
				}
				task.setCompletionDate(modificationDate);
			} else {
				task.setCompletionDate(null);
			}
		}
		task.setUrl(taskRepository.getRepositoryUrl() + IJiraClient.TICKET_URL + taskData.getTaskId());
		if (!taskData.isPartial()) {
			task.setAttribute(TASK_KEY_SUPPORTS_SUBTASKS, Boolean.toString(taskDataHandler.supportsSubtasks(taskData)));
			Date date = task.getModificationDate();
			task.setAttribute(TASK_KEY_UPDATE_DATE, (date != null) ? JiraUtil.toJiraTime(date) + "" : null); //$NON-NLS-1$
		}
	}

	@Override
	public boolean hasTaskChanged(TaskRepository taskRepository, ITask task, TaskData taskData) {
		TaskMapper mapper = getTaskMapping(taskData);
		if (taskData.isPartial()) {
			return mapper.hasChanges(task);
		} else {
			Date repositoryDate = mapper.getModificationDate();
			Date localDate = JiraUtil.parseDate(task.getAttribute(TASK_KEY_UPDATE_DATE));
			if (repositoryDate != null && repositoryDate.equals(localDate)) {
				return false;
			}
			return true;
		}
	}

	@Override
	public Collection<TaskRelation> getTaskRelations(TaskData taskData) {
		TaskAttribute attribute = taskData.getRoot().getAttribute(JiraTaskDataHandler.ATTRIBUTE_BLOCKED_BY);
		if (attribute != null) {
			List<TaskRelation> result = new ArrayList<TaskRelation>();
			StringTokenizer t = new StringTokenizer(attribute.getValue(), ", "); //$NON-NLS-1$
			while (t.hasMoreTokens()) {
				result.add(TaskRelation.subtask(t.nextToken()));
			}
			return result;
		}
		return Collections.emptySet();
	}

	@Override
	public JiraTaskMapper getTaskMapping(TaskData taskData) {
		TaskRepository taskRepository = taskData.getAttributeMapper().getTaskRepository();
		IJiraClient client = (taskRepository != null) ? getClientManager().getJiraClient(taskRepository) : null;
		return new JiraTaskMapper(taskData, client);
	}

	@Override
	public boolean canGetTaskHistory(TaskRepository repository, ITask task) {
		return Version.XML_RPC.name().equals(repository.getVersion());
	}

	@Override
	public TaskHistory getTaskHistory(TaskRepository repository, ITask task, IProgressMonitor monitor)
			throws CoreException {
		try {
			IJiraClient client = getClientManager().getJiraClient(repository);
			List<JiraComment> comments = client.getComments(getTicketId(task.getTaskId()), monitor);
			TaskHistory history = new TaskHistory(repository, task);
			TaskRevision revision = null;
			for (JiraComment comment : comments) {
				String id = comment.getCreated().getTime() + ""; //$NON-NLS-1$
				if (revision == null || !id.equals(revision.getId())) {
					revision = new TaskRevision(id, comment.getCreated(), repository.createPerson(comment.getAuthor()));
					history.add(revision);
				}
				JiraAttribute attribute = JiraAttribute.getByJiraKey(comment.getField());
				if (attribute != null) {
					String fieldName = attribute.toString();
					if (fieldName.endsWith(":")) { //$NON-NLS-1$
						fieldName = fieldName.substring(0, fieldName.length() - 1);
					}
					TaskRevision.Change change = new TaskRevision.Change(attribute.getJiraKey(), fieldName,
							comment.getOldValue(), comment.getNewValue());
					revision.add(change);
				}
			}
			return history;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Throwable e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		}
	}

	@Override
	public boolean canDeleteTask(TaskRepository repository, ITask task) {
		return hasRichEditor(repository);
	}

	@Override
	public IStatus deleteTask(TaskRepository repository, ITask task, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		IJiraClient client = getClientManager().getJiraClient(repository);
		try {
			client.deleteTicket(getTicketId(task.getTaskId()), monitor);
		} catch (JiraException e) {
			throw new CoreException(JiraCorePlugin.toStatus(e, repository));
		}
		return Status.OK_STATUS;
	}
}
