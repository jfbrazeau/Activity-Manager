package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.DefaultFeatureAccessManagerImpl;
import org.activitymgr.ui.web.logic.impl.ITaskCreationPatternHandler;
import org.activitymgr.ui.web.logic.impl.LabelLogicImpl;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class TaskChooserLogicImpl extends AbstractLogicImpl<ITaskChooserLogic.View> implements ITaskChooserLogic {
	
	private static final String NONE_PATTERN_ID = "none";
	
	private static final String NONE_PATTERN_LABEL = "None";

	private Collection<Long> alreadySelectedTaskIds;

	public TaskChooserLogicImpl(AbstractLogicImpl<?> parent, Collection<Long> selectedTaskIds, Collaborator contributor, Calendar monday) {
		super(parent);
		// Remember already selected task ids
		this.alreadySelectedTaskIds = selectedTaskIds;
		// Register the tree content provider
		TaskTreeContentProvider treeContentProvider = new TaskTreeContentProvider(this, getContext());
		getView().setTasksTreeProviderCallback(getContext().buildTransactionalWrapper(treeContentProvider, ITreeContentProviderCallback.class));
		
		// Retrieve recent tasks labels
		Calendar from = (Calendar) monday.clone();
		from.add(Calendar.DATE, -7);
		Calendar to = (Calendar) monday.clone();
		to.add(Calendar.DATE, 6);
		try {
			final Map<Long, Task> recentTasksMap = new HashMap<Long, Task>();
			// Retrieve recent tasks
			Task[] recentTasks = getModelMgr().getContributedTasks(contributor, from, to);
			for (Task recentTask : recentTasks) {
				recentTasksMap.put(recentTask.getId(), recentTask);
			}
			// Add selected ID (if missing)
			for (Long selectedTaskId : alreadySelectedTaskIds) {
				if (!recentTasksMap.containsKey(selectedTaskId)) {
					recentTasksMap.put(selectedTaskId, getModelMgr().getTask(selectedTaskId));
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
			ITableCellProviderCallback<Long> recentTaskCallback = new AbstractSafeTableCellProviderCallback<Long>(this, getContext()) {
				private final Collection<String> PROPERTY_IDS = Arrays.asList(new String[] { TaskTreeContentProvider.NAME_PROPERTY_ID });
				@Override
				protected Collection<Long> unsafeGetRootElements() throws Exception {
					return recentTasksIds;
				}
				@Override
				protected IView<?> unsafeGetCell(
						Long taskId, String propertyId) throws Exception {
					return new LabelLogicImpl((AbstractLogicImpl<?>) getSource(), "[" + tasksCodePathMap.get(taskId) + "] " + recentTasksMap.get(taskId).getName()).getView();
				}
				@Override
				protected Collection<String> unsafeGetPropertyIds() {
					return PROPERTY_IDS;
				}
				@Override
				protected boolean unsafeContains(Long taskId) {
					return recentTasksIds.contains(taskId);
				}
			};
			getView().setRecentTasksProviderCallback(recentTaskCallback);

			IConfigurationElement[] creationPatternCfgs = getCreationPatternCfgs();
			final List<String> creationPatternIds = new ArrayList<String>();
			final Map<String, String> creationPatternsLabels = new HashMap<String, String>();
			// Add non pattern
			creationPatternIds.add(NONE_PATTERN_ID);
			creationPatternsLabels.put(NONE_PATTERN_ID, NONE_PATTERN_LABEL);
			for (IConfigurationElement creationPatternCfg : creationPatternCfgs) {
				String id = creationPatternCfg.getAttribute("id");
				creationPatternsLabels.put(id, creationPatternCfg.getAttribute("label"));
				creationPatternIds.add(id);
			}
			ITableCellProviderCallback<String> creationPatternsCallback = new AbstractSafeTableCellProviderCallback<String>(this, getContext()) {
				private final Collection<String> PROPERTY_IDS = Arrays.asList(new String[] { TaskTreeContentProvider.NAME_PROPERTY_ID });
				@Override
				protected Collection<String> unsafeGetRootElements() throws Exception {
					return creationPatternIds;
				}
				@Override
				protected IView<?> unsafeGetCell(
						String patternId, String propertyId) throws Exception {
					return new LabelLogicImpl((AbstractLogicImpl<?>) getSource(), creationPatternsLabels.get(patternId)).getView();
				}
				@Override
				protected Collection<String> unsafeGetPropertyIds() {
					return PROPERTY_IDS;
				}
				@Override
				protected boolean unsafeContains(String patternId) {
					return creationPatternIds.contains(patternId);
				}
			};
			getView().setCreationPatternProviderCallback(creationPatternsCallback);
			
			// Reset button state & status label
			onSelectionChanged(-1);
		
			// Open the window
			getRoot().getView().openWindow(getView());
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving recent tasks", e);
		}

	}

	private IConfigurationElement[] getCreationPatternCfgs() {
		return Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.taskCreationPatternHandler");
	}

	@Override
	public void onSelectionChanged(long taskId) {
		checkDialogRules(taskId, getView().getNewTaskName(), getView().getNewTaskCode());
	}

	@Override
	public void onNewTaskCheckboxClicked() {
		checkDialogRules(getView().getSelectedTaskId(), getView()
				.getNewTaskName(), getView().getNewTaskCode());
	}
	
	@Override
	public void onNewTaskNameChanged(String newTaskName) {
		checkDialogRules(getView().getSelectedTaskId(), newTaskName, getView()
				.getNewTaskCode());
	}

	@Override
	public void onNewTaskCodeChanged(String newTaskCode) {
		checkDialogRules(getView().getSelectedTaskId(), getView()
				.getNewTaskName(), newTaskCode);
	}

	private void checkDialogRules(Long selectedTaskId, String newTaskName, String newTaskCode) {
		Task selectedTask = getModelMgr().getTask(selectedTaskId);
		String newStatus = "";
		boolean okButtonEnabled = false;
		boolean newTaskFormEnabled = false;
		boolean newTaskFieldsEnabled = false;
		if (selectedTask != null) {
			if (alreadySelectedTaskIds.contains(selectedTask.getId())) {
				newStatus = "This task is already selected";
			} else if (!getModelMgr().isLeaf(selectedTask.getId())) {
				newTaskFormEnabled = true;
				if (getView().isNewTaskChecked()) {
					newTaskFieldsEnabled = true;
					boolean codeInUse = false;
					if (newTaskCode != null && !"".equals(newTaskCode.trim())) {
						if (getModelMgr().getTask(selectedTask.getFullPath(), newTaskCode) != null) {
							newStatus = "This code is already in use";
							codeInUse = true;
						}
					}
					if (!codeInUse) {
						if (newTaskName == null || "".equals(newTaskName.trim())) {
							newStatus = "Enter a task name";
						}
						else {
							okButtonEnabled = true;
						}
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
		getView().setNewTaskFieldsEnabled(newTaskFieldsEnabled);
	}

	@Override
	public void onOkButtonClicked(long taskId) {
		try {
			if (getModelMgr().isLeaf(taskId)) {
				((AbstractContributionTabLogicImpl) getParent()).addTasks(taskId);
			}
			else {
				Task parent = getModelMgr().getTask(taskId);
				Task newTask = getContext().getBeanFactory().newTask();
				newTask.setName(getView().getNewTaskName());
				String code = getView().getNewTaskCode().trim();
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
				if (!NONE_PATTERN_ID.equals(patternId)) {
					IConfigurationElement[] creationPatternCfgs = getCreationPatternCfgs();
					ITaskCreationPatternHandler patternHandler = null;
					for (IConfigurationElement cfg : creationPatternCfgs) {
						if (patternId.equals(cfg.getAttribute("id"))) {
							patternHandler = (ITaskCreationPatternHandler) cfg.createExecutableExtension("class");
							break;
						}
					}
					List<Task> createdTasks = patternHandler.handle(getContext(), newTask);
					for (Task subTask : createdTasks) {
						long id = subTask.getId();
						if (!selectedTaskIds.contains(id)) {
							selectedTaskIds.add(id);
						}
					}
				}
				// If no task has been selected, auto select a task
				if (selectedTaskIds.size() == 0) {
					Task[] subTasks = getModelMgr().getSubTasks(newTask);
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
			handleError(e);
		} catch (CoreException e) {
			handleError(e);
		}
	}

	@Override
	public void onRecentTaskClicked(long taskId) {
		getView().selectTask(taskId);
	}

}