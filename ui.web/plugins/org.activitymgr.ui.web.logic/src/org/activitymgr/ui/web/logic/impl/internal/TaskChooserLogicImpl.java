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
		TaskTreeContentProvider treeContentCallback = new TaskTreeContentProvider(this, getContext(), getModelMgr());
		getView().setTreeContentProviderCallback(getContext().buildTransactionalWrapper(treeContentCallback, ITreeContentProviderCallback.class));
		
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
					System.out.println("compare(" + t1 + ", " + t2 + ")");
					return tasksCodePathMap.get(t1.getId()).compareTo(tasksCodePathMap.get(t2.getId()));
				}
			});
			final List<Task> recentTasksList = Arrays.asList(recentTasks);
			IListContentProviderCallback<Task> recentTaskCallback = new AbstractSafeListContentProviderCallback<Task>(this, getContext().getEventBus()) {
				@Override
				protected Collection<Task> unsafeGetRootElements() throws Exception {
					return recentTasksList;
				}
				@Override
				public String unsafeGetText(Task task, String propertyId) {
					return "[" + tasksCodePathMap.get(task.getId()) + "] " + task.getName();
				}
				@Override
				public Collection<String> getPropertyIds() {
					return DEFAULT_PROPERTY_IDS;
				}
			};
			getView().setRecentTasksProviderCallback(recentTaskCallback);

			// Reset button state & status label
			onSelectionChanged(null);
			
			// A preload of recent tasks must be performed in the vaadin tree. Otherwise, after
			// having clicked on a recent task, it does not become selected in the tree.
			if (recentTasksList.size() > 0) {
				getView().preloadTreeItems(recentTasksList);
			}
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving recent tasks", e);
		}

	}

	@Override
	public void onSelectionChanged(Task task) {
		checkDialogRules((Task) task, getView().getNewTaskName());
	}

	@Override
	public void onNewTaskCheckboxClicked() {
		Task selectedTask = (Task) getView().getSelectedTask();
		checkDialogRules(selectedTask, getView().getNewTaskName());
	}
	
	@Override
	public void onNewTaskNameChanged(String newTaskName) {
		Task selectedTask = (Task) getView().getSelectedTask();
		checkDialogRules(selectedTask, newTaskName);
	}

	private void checkDialogRules(Task selectedTask, String newTaskName) {
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

	@Override
	public void onOkButtonClicked(Task task) {
		try {
			if (getModelMgr().isLeaf(task.getId())) {
				((ContributionsTabLogicImpl) getParent()).addTask(task);
			}
			else {
				Task newTask = getContext().getBeanFactory().newTask();
				newTask.setName(getView().getNewTaskName());
				String code = newTask.getName().trim().replaceAll(" ", "").toUpperCase();
				if (code.length() > 7) {
					code = code.substring(0, 7);
				}
				newTask.setCode('$' + code);
				getModelMgr().createTask(task, newTask);
				((ContributionsTabLogicImpl) getParent()).addTask(newTask);
			}
		} catch (ModelException e) {
			handleError(e);
		}
	}

	@Override
	public void onRecentTaskClicked(Task task) {
		List<Task> tasks = getParentTasks(task);
		getView().expandTasks(tasks);
		getView().selectTask(task);
	}

	private List<Task> getParentTasks(Task task) {
		List<Task> tasks = new ArrayList<Task>();
		while (task != null) {
			tasks.add(0, task);
			task = getModelMgr().getParentTask(task);
		}
		return tasks;
	}

}