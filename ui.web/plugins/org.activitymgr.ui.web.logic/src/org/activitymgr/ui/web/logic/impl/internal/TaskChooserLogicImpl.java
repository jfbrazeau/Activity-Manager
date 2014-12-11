package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeListContentProviderCallback;

public class TaskChooserLogicImpl extends AbstractLogicImpl<ITaskChooserLogic.View> implements ITaskChooserLogic {
	
	private List<Long> alreadySelectedTaskIds;

	public TaskChooserLogicImpl(AbstractLogicImpl<?> parent, List<Long> selectedTaskIds, Collaborator contributor, Calendar monday) {
		super(parent);
		// Remember already selected task ids
		this.alreadySelectedTaskIds = selectedTaskIds;
		// Register the tree content provider
		TaskTreeContentProvider treeContentProvider = new TaskTreeContentProvider(this, getContext(), getModelMgr());
		getView().setTreeContentProviderCallback(getContext().buildTransactionalWrapper(treeContentProvider, ITreeContentProviderCallback.class));
		
		// Retrieve recent tasks labels
		Calendar from = (Calendar) monday.clone();
		from.add(Calendar.DATE, -7);
		Calendar to = (Calendar) monday.clone();
		to.add(Calendar.DATE, 6);
		final Map<Long, String> tasksCodePathMap = new HashMap<Long, String>();
		try {
			Task[] recentTasks = getModelMgr().getContributedTaskContainers(contributor, from, to);
			for (Task recentTask : recentTasks) {
				String taskCodePath = getModelMgr().getTaskCodePath(recentTask);
				tasksCodePathMap.put(recentTask.getId(), taskCodePath);
			}
			Arrays.sort(recentTasks, new Comparator<Task>() {
				@Override
				public int compare(Task t1, Task t2) {
					return tasksCodePathMap.get(t1.getId()).compareTo(tasksCodePathMap.get(t2.getId()));
				}
			});
			final Map<Long, Task> recentTasksMap = new HashMap<Long, Task>();
			final List<Long> recentTasksIds = new ArrayList<Long>();
			for (Task recentTask : recentTasks) {
				recentTasksIds.add(recentTask.getId());
				recentTasksMap.put(recentTask.getId(), recentTask);
			}
			IListContentProviderCallback<Long> recentTaskCallback = new AbstractSafeListContentProviderCallback<Long>(this, getContext().getEventBus()) {
				@Override
				protected Collection<Long> unsafeGetRootElements() throws Exception {
					return recentTasksIds;
				}
				@Override
				public String unsafeGetText(Long taskId, String propertyId) {
					return "[" + tasksCodePathMap.get(taskId) + "] " + recentTasksMap.get(taskId).getName();
				}
				@Override
				public Collection<String> getPropertyIds() {
					return DEFAULT_PROPERTY_IDS;
				}
				@Override
				protected boolean unsafeContains(Long taskId) {
					return recentTasksIds.contains(taskId);
				}
			};
			getView().setRecentTasksProviderCallback(recentTaskCallback);

			// Reset button state & status label
			onSelectionChanged(-1);
		
			// Open the window
			getRoot().getView().openWindow(getView());
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving recent tasks", e);
		}

	}

	@Override
	public void onSelectionChanged(long taskId) {
		checkDialogRules(taskId, getView().getNewTaskName());
	}

	@Override
	public void onNewTaskCheckboxClicked() {
		Long selectedTaskId = (Long) getView().getSelectedTaskId();
		checkDialogRules(selectedTaskId, getView().getNewTaskName());
	}
	
	@Override
	public void onNewTaskNameChanged(String newTaskName) {
		Long selectedTaskId = (Long) getView().getSelectedTaskId();
		checkDialogRules(selectedTaskId, newTaskName);
	}

	private void checkDialogRules(Long selectedTaskId, String newTaskName) {
		Task selectedTask = getModelMgr().getTask(selectedTaskId);
		String newStatus = "";
		boolean okButtonEnabled = false;
		boolean newTaskFormEnabled = false;
		boolean newTaskNameEnabled = false;
		if (selectedTask != null) {
			if (alreadySelectedTaskIds.contains(selectedTask.getId())) {
				newStatus = "This task is already selected";
			} else if (!getModelMgr().isLeaf(selectedTask.getId())) {
				newTaskFormEnabled = true;
				if (getView().isNewTaskChecked()) {
					newTaskNameEnabled = true;
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

	@Override
	public void onOkButtonClicked(long taskId) {
		try {
			if (getModelMgr().isLeaf(taskId)) {
				((ContributionsTabLogicImpl) getParent()).addTask(taskId);
			}
			else {
				Task parent = getModelMgr().getTask(taskId);
				Task newTask = getContext().getBeanFactory().newTask();
				newTask.setName(getView().getNewTaskName());
				String code = newTask.getName().trim().replaceAll(" ", "").toUpperCase();
				if (code.length() > 7) {
					code = code.substring(0, 7);
				}
				newTask.setCode('$' + code);
				getModelMgr().createTask(parent, newTask);
				((ContributionsTabLogicImpl) getParent()).addTask(newTask);
			}
		} catch (ModelException e) {
			handleError(e);
		}
	}

	@Override
	public void onRecentTaskClicked(long taskId) {
		getView().selectTask(taskId);
	}

}