package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.ModelException;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeLabelProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeListContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.LogicContext;

public class TaskChooserLogicImpl extends AbstractLogicImpl<ITaskChooserLogic.View> implements ITaskChooserLogic {
	
	private List<Long> alreadySelectedTaskIds;
	private HashMap<Long, Task> recentTasks;

	public TaskChooserLogicImpl(AbstractLogicImpl<?> parent, List<Long> selectedTaskIds, Collaborator contributor, Calendar monday) {
		super(parent);
		// Remember already selected task ids
		this.alreadySelectedTaskIds = selectedTaskIds;
		// Register the tree content provider
		TaskTreeContentProvider treeContentCallback = new TaskTreeContentProvider(this, getContext(), getModelMgr());
		getView().setTreeContentProviderCallback(getContext().buildTransactionalWrapper(treeContentCallback, ITreeContentProviderCallback.class));
		
		// Retrieve recent tasks labels
		recentTasks = new HashMap<Long, Task>();
		final List<String> recentTaskIds = new ArrayList<String>();
		Calendar from = (Calendar) monday.clone();
		from.add(Calendar.DATE, -7);
		Calendar to = (Calendar) monday.clone();
		to.add(Calendar.DATE, 6);
		final Map<String, String> tasksCodePathMap = new HashMap<String, String>();
		try {
			Task[] recentTasksList = getModelMgr().getContributedTaskContainers(contributor, from, to);
			for (Task recentTask : recentTasksList) {
				String taskId = String.valueOf(recentTask.getId());
				recentTaskIds.add(taskId);
				recentTasks.put(recentTask.getId(), recentTask);
				String taskCodePath = getModelMgr().getTaskCodePath(recentTask);
				tasksCodePathMap.put(taskId, taskCodePath);
			}
			Collections.sort(recentTaskIds, new Comparator<String>() {
				@Override
				public int compare(String id1, String id2) {
					return tasksCodePathMap.get(id1).compareTo(tasksCodePathMap.get(id2));
				}
			});
			
		} catch (DAOException e) {
			throw new IllegalStateException("Unexpected error while retrieving recent tasks", e);
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving recent tasks", e);
		}
		IListContentProviderCallback recentTaskCallback = new AbstractSafeListContentProviderCallback(this, getContext().getEventBus()) {
			@Override
			protected Collection<String> unsafeRootItemIds() throws Exception {
				return recentTaskIds;
			}
			
			@Override
			protected ILabelProviderCallback unsafeGetLabelProvider(final String itemId)
					throws Exception {
				final Task task = recentTasks.get(Long.parseLong(itemId));
				AbstractSafeLabelProviderCallback callback = new AbstractSafeLabelProviderCallback(getSource(), getEventBus()) {
					@Override
					protected String unsafeGetText() throws Exception {
						return "[" + tasksCodePathMap.get(itemId) + "] " + task.getName();
					}
					
					@Override
					protected Icon unsafeGetIcon() throws Exception {
						return null;
					}
				};
				return getContext().buildTransactionalWrapper(callback, ILabelProviderCallback.class);
			}
		};
		getView().setRecentTasksProviderCallback(recentTaskCallback);

		// Reset button state & status label
		onSelectionChanged(null);

		// A preload of recent tasks must be performed in the vaadin tree. Otherwise, after
		// having clicked on a recent task, it does not become selected in the tree.s
		if (recentTaskIds.size() > 0) {
			getView().preloadTreeItems(recentTaskIds);
		}
	}

	@Override
	public void onSelectionChanged(Long taskId) {
		checkDialogRules(taskId, getView().getNewTaskName());
	}

	@Override
	public void onNewTaskCheckboxClicked() {
		String selectedTaskId = getView().getSelectedTaskId();
		checkDialogRules(selectedTaskId != null ? Long.parseLong(selectedTaskId) : null, getView().getNewTaskName());
	}
	
	@Override
	public void onNewTaskNameChanged(String newTaskName) {
		String selectedTaskId = getView().getSelectedTaskId();
		checkDialogRules(selectedTaskId != null ? Long.parseLong(selectedTaskId) : null, newTaskName);
	}

	private void checkDialogRules(Long selectedTaskId, String newTaskName) {
		System.out.println("checkDialogRules(" + selectedTaskId + ")");
		try {
			String newStatus = "";
			boolean okButtonEnabled = false;
			boolean newTaskFormEnabled = false;
			boolean newTaskNameEnabled = false;
			if (selectedTaskId != null) {
				if (alreadySelectedTaskIds.contains(selectedTaskId)) {
					newStatus = "This task is already selected";
				} else if (!getModelMgr().isLeaf(selectedTaskId)) {
					newTaskFormEnabled = true;
					if (getView().isNewTaskChecked()) {
						newTaskNameEnabled = true;
						System.out.println("New task name: '" + newTaskName + "'");
						if (newTaskName == null || "".equals(newTaskName.trim())) {
							newStatus = "Enter a task name";
						}
						else {
							okButtonEnabled = true;
						}
					}
					else {
						newStatus = "You cannot select a container task";
					}
				}
				else {
					okButtonEnabled = true;
				}
			}
			getView().setStatus(newStatus);
			getView().setOkButtonEnabled(okButtonEnabled);
			getView().setNewTaskFormEnabled(newTaskFormEnabled);
			getView().setNewTaskNameEnabled(newTaskNameEnabled);
		}
		catch (DAOException e) {
			handleError(e);
		}
	}

	@Override
	public void onOkButtonClicked(long taskId) {
		try {
			if (getModelMgr().isLeaf(taskId)) {
				((ContributionsTabLogicImpl) getParent()).addTask(taskId);
			}
			else {
				Task parentTask = getModelMgr().getTask(taskId);
				Task newTask = new Task();
				newTask.setName(getView().getNewTaskName());
				String code = newTask.getName().trim().replaceAll(" ", "").toUpperCase();
				if (code.length() > 7) {
					code = code.substring(0, 7);
				}
				newTask.setCode('$' + code);
				getModelMgr().createTask(parentTask, newTask);
				((ContributionsTabLogicImpl) getParent()).addTask(newTask.getId());
			}
		} catch (DAOException e) {
			handleError(e);
		} catch (ModelException e) {
			handleError(e);
		}
	}

	@Override
	public void onRecentTaskClicked(long taskId) {
		try {
			Task cursor = recentTasks.get(taskId);
			List<Long> ids = getParentTaskIds(cursor);
			getView().expandTasks(ids);
			getView().selectTask(taskId);
		} catch (DAOException e) {
			handleError(e);
		}
	}

	private List<Long> getParentTaskIds(Task task) throws DAOException {
		List<Long> ids = new ArrayList<Long>();
		while (task != null) {
			ids.add(0, task.getId());
			task = getModelMgr().getParentTask(task);
		}
		return ids;
	}

}

class TaskTreeContentProvider extends AbstractSafeTreeContentProviderCallback {

	private IModelMgr modelMgr;
	private LogicContext context;

	public TaskTreeContentProvider(ILogic<?> source, LogicContext context, IModelMgr modelMgr) {
		super(source, context.getEventBus());
		this.modelMgr = modelMgr;
		this.context = context;
	}

	@Override
	protected ILabelProviderCallback unsafeGetLabelProvider(final String itemId)
			throws Exception {
		AbstractSafeLabelProviderCallback callback = new AbstractSafeLabelProviderCallback(getSource(), getEventBus()) {
			
			@Override
			protected String unsafeGetText() throws Exception {
				return modelMgr.getTask(Long.parseLong(itemId)).getName();
			}
			
			@Override
			protected Icon unsafeGetIcon() throws Exception {
				return null;
			}
		};
		return context.buildTransactionalWrapper(callback, ILabelProviderCallback.class);
	}

	@Override
	protected Collection<String> unsafeGetChildren(String itemId)
			throws Exception {
		Task[] subTasks = modelMgr.getSubtasks(itemId == null ? null : Long.parseLong(itemId));
		Collection<String> result = new ArrayList<String>();
		for (Task subTask : subTasks) {
			result.add(String.valueOf(subTask.getId()));
		}
		return result;
	}

	@Override
	protected Collection<String> unsafeRootItemIds() throws Exception {
		return unsafeGetChildren(null);
	}

	@Override
	protected boolean unsafeIsRoot(String itemId) throws Exception {
		return unsafeRootItemIds().contains(itemId);
	}
	
}