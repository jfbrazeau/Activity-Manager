package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IConstraintsValidator;
import org.activitymgr.ui.web.logic.IConstraintsValidator.ErrorStatus;
import org.activitymgr.ui.web.logic.IConstraintsValidator.IStatus;
import org.activitymgr.ui.web.logic.IContributionTaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITaskCreationPatternHandler;

import com.google.inject.Inject;

public class ContributionTaskChooserLogicImpl extends
		AbstractTaskChooserLogicImpl<IContributionTaskChooserLogic.View>
		implements IContributionTaskChooserLogic {
	
	private Collection<Long> alreadySelectedTaskIds;
	
	@Inject
	private IDTOFactory dtoFactory;
	
	@Inject
	private Map<String, ITaskCreationPatternHandler> taskCreationPatternHandlers;
	
	@Inject
	private Set<IConstraintsValidator> constraintsValidators;

	private TaskTreeCellProvider treeContentProvider;
	
	private String newTaskName;

	private String newTaskCode;

	private boolean newTaskChecked;

	public ContributionTaskChooserLogicImpl(AbstractLogicImpl<?> parent,
			Long taskIdToExpand, Collection<Long> selectedTaskIds,
			Task[] recentTasks) {
		super(parent, taskIdToExpand, null);
		// Remember already selected task ids
		this.alreadySelectedTaskIds = selectedTaskIds;
		
		// Retrieve recent tasks labels
		try {
			final Map<Long, Task> recentTasksMap = new HashMap<Long, Task>();
			// Retrieve recent tasks
			for (Task recentTask : recentTasks) {
				recentTasksMap.put(recentTask.getId(), recentTask);
			}
			// Add selected ID (if missing)
			for (Long selectedTaskId : alreadySelectedTaskIds) {
				if (!recentTasksMap.containsKey(selectedTaskId)) {
					recentTasksMap.put(selectedTaskId,
							getModelMgr().getTask(selectedTaskId));
				}
			}
			// Retrieve task code path
			final Map<Long, String> tasksCodePathMap = new HashMap<Long, String>();
			final List<Long> recentTasksIds = new ArrayList<Long>(recentTasksMap.keySet());
			for (Long taskId : recentTasksIds) {
				Task task = recentTasksMap.get(taskId);
				String taskCodePath = getModelMgr().getTaskCodePath(task);
				tasksCodePathMap.put(taskId, taskCodePath);
			}
			Collections.sort(recentTasksIds, new Comparator<Long>() {
				@Override
				public int compare(Long taskId1, Long taskId2) {
					Task task1 = recentTasksMap.get(taskId1);
					Task task2 = recentTasksMap.get(taskId2);
					String fullPath1 = task1.getFullPath();
					String fullPath2 = task2.getFullPath();
					return fullPath1.compareTo(fullPath2);
				}
			});
			Map<Long, String> recentTasksLabelsMap = new LinkedHashMap<Long, String>();
			for (Long taskId : recentTasksIds) {
				recentTasksLabelsMap.put(taskId, "[" + tasksCodePathMap.get(taskId) + "] " + recentTasksMap.get(taskId).getName());
			}
			getView().setRecentTasks(recentTasksLabelsMap);

			// Pattern handler list
			ArrayList<String> taskCreationPatternIds = new ArrayList<String>(taskCreationPatternHandlers.keySet());
			Collections.sort(taskCreationPatternIds);
			Map<String, String> taskCreationPatternHandlersLabelsMap = new HashMap<String, String>();
			for (String patternId : taskCreationPatternIds) {
				taskCreationPatternHandlersLabelsMap.put(patternId, taskCreationPatternHandlers.get(patternId).getLabel());
			}
			getView().setCreationPatterns(taskCreationPatternHandlersLabelsMap);

			// Reset button state & status label
			onSelectionChanged(taskIdToExpand);
		
			// Open the window
			getRoot().getView().openWindow(getView());

			// Update state
			updateUI();
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving recent tasks", e);
		}

	}
	
	@Override
	public void onTaskFilterChanged(String filter) {
		filter = filter.trim();
		if (treeContentProvider != null) {
			treeContentProvider.dispose();
		}
		// Register the tree content provider
		treeContentProvider = new TaskTreeCellProvider(this, filter, true);
		getView().setTasksTreeProviderCallback(buildTransactionalWrapper(treeContentProvider, ITreeContentProviderCallback.class));
		if (!"".equals(filter)) {
			Task task = getModelMgr().getFirstTaskMatching(filter);
			if (task != null) {
				getView().expandToTask(task.getId());
			}
		}
	}

	@Override
	public void onNewTaskCheckboxClicked() {
		newTaskChecked = !newTaskChecked;
		updateUI();
	}

	@Override
	public void onNewTaskNameChanged(String newTaskName) {
		this.newTaskName = newTaskName;
		updateUI();
	}

	@Override
	public void onNewTaskCodeChanged(String newTaskCode) {
		this.newTaskCode = newTaskCode;
		updateUI();
	}

	@Override
	protected IStatus checkDialogRules() throws ModelException {
		IStatus status = super.checkDialogRules();
		if (status.isError()) {
			return status;
		}
		Task selectedTask = getSelectedTaskId() != null ? getModelMgr()
				.getTask(getSelectedTaskId()) : null;
		String newStatus = null;
		boolean newTaskFieldsEnabled = false;
		if (selectedTask != null) {
			boolean isLeaf = getModelMgr().isLeaf(selectedTask.getId());
			if (newTaskChecked) {
				newTaskFieldsEnabled = true;
				// Check constraints
				for (IConstraintsValidator cv : constraintsValidators) {
					status = cv.canCreateSubTaskUnder(selectedTask);
					if (status.isError()) {
						newStatus = "It's not possible to create a sub task under the selected task :\n" + status.getErrorReason();
						break;
					}
				}
				// If constraints OK, check following rules
				if (newStatus == null) {
					if (newTaskCode != null && !"".equals(newTaskCode = newTaskCode.trim()) && getModelMgr().getTask(selectedTask.getFullPath(), newTaskCode) != null) {
						newStatus = "This code is already in use";
					}
					else if (newTaskName == null || "".equals(newTaskName.trim())) {
						newStatus = "Enter a task name";
					}
				}
			} else if (!isLeaf) {
				newStatus = "You cannot select a container task";
			} else if (alreadySelectedTaskIds != null
					&& alreadySelectedTaskIds.contains(selectedTask.getId())) {
				newStatus = "This task is already selected";
			}
		}
		getView().setNewTaskFieldsEnabled(newTaskFieldsEnabled);
		if (newStatus != null) {
			return new ErrorStatus(newStatus);
		} else {
			return IConstraintsValidator.OK_STATUS;
		}
	}

	@Override
	public void onOkButtonClicked(long taskId) {
		try {
			if (!newTaskChecked) {
				((AbstractContributionTabLogicImpl) getParent()).addTasks(taskId);
			}
			else {
				Task parent = getModelMgr().getTask(taskId);
				Task newTask = dtoFactory.newTask();
				newTask.setName(newTaskName.trim());
				String code = newTaskCode != null ? newTaskCode.trim() : "";
				if ("".equals(code)) {
					code = newTask.getName().trim().replaceAll(" ", "").toUpperCase();
					if (code.length() > 7) {
						code = code.substring(0, 7);
					}
					code = '$' + code;
				}
				newTask.setCode(code);
				getModelMgr().createTask(parent, newTask);
				
				// Init task to select list
				List<Long> selectedTaskIds = new ArrayList<Long>();

 				// Task creation pattern management
				String patternId = getView().getSelectedTaskCreationPatternId();
				if (patternId != null) {
					ITaskCreationPatternHandler handler = taskCreationPatternHandlers.get(patternId);
					List<Task> createdTasks = handler.handle(getContext(), newTask);
					for (Task subTask : createdTasks) {
						long id = subTask.getId();
						if (!selectedTaskIds.contains(id)) {
							selectedTaskIds.add(id);
						}
					}
				}
				// If no task has been selected (which may occur if the creation pattern handler doesn't return
				// anything), auto select a task
				if (selectedTaskIds.size() == 0) {
					Task[] subTasks = getModelMgr().getSubTasks(newTask.getId());
					selectedTaskIds.add(subTasks.length == 0 ? newTask.getId() : subTasks[0].getId());
				}
				// Turn the selection list into an array
				long[] selectedTaskIdsArray = new long[selectedTaskIds.size()];
				int i = 0;
				for (long id : selectedTaskIds) {
					selectedTaskIdsArray[i++] = id;
				}
				// Add task to the contribution tab
				((AbstractContributionTabLogicImpl) getParent()).addTasks(selectedTaskIdsArray);
			}
		} catch (ModelException e) {
			doThrow(e);
		}
	}

	@Override
	public void onRecentTaskClicked(long taskId) {
		getView().selectTask(taskId);
	}

}