package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.DbException;
import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeLabelProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeListContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractWeekContributionsProviderExtension;
import org.activitymgr.ui.web.logic.impl.LogicContext;

public class TaskChooserLogicImpl extends AbstractLogicImpl<ITaskChooserLogic.View> implements ITaskChooserLogic {
	
	private List<Long> alreadySelectedTaskIds;
	private HashMap<Long, Task> recentTasks;

	public TaskChooserLogicImpl(AbstractLogicImpl<?> parent, List<Long> selectedTaskIds, Collaborator contributor, AbstractWeekContributionsProviderExtension weekContributionsPrvider, Calendar monday) {
		super(parent);
		// Remember already selected task ids
		this.alreadySelectedTaskIds = selectedTaskIds;
		// Register the tree content provider
		TaskTreeContentProvider treeContentCallback = new TaskTreeContentProvider(this, getContext(), getModelMgr());
		getView().setTreeContentProviderCallback(getContext().buildTransactionalWrapper(treeContentCallback, ITreeContentProviderCallback.class));
		
		// Retrieve recent tasks labels
		TaskContributions[] weekContributions = weekContributionsPrvider.getWeekContributions(getContext(), contributor, monday);
		recentTasks = new HashMap<Long, Task>();
		final Collection<String> recentTaskIds = new ArrayList<String>();
		for (TaskContributions tc : weekContributions) {
			recentTaskIds.add(String.valueOf(tc.getTask().getId()));
			recentTasks.put(tc.getTask().getId(), tc.getTask());
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
						return task.getName();
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
		try {
			String newStatus = "";
			boolean okButtonEnabled = false;
			boolean newTaskFormEnabled = false;
			if (taskId != null) {
				if (alreadySelectedTaskIds.contains(taskId)) {
					newStatus = "This task is already selected";
				} else if (!getModelMgr().isLeaf(taskId)) {
					newTaskFormEnabled = true;
					if (getView().isNewTaskChecked()) {
						String newTaskName = getView().getNewTaskName();
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
			getView().setStatus(newStatus + "-" + getView().isNewTaskChecked());
			getView().setOkButtonEnabled(okButtonEnabled);
			getView().setNewTaskFormEnabled(newTaskFormEnabled);
		}
		catch (DbException e) {
			handleError(e);
		}
	}

	@Override
	public void onTaskChosen(long taskId) {
		((ContributionsTabLogicImpl) getParent()).addTask(taskId);
	}

	@Override
	public void onRecentTaskClicked(long taskId) {
		try {
			Task cursor = recentTasks.get(taskId);
			List<Long> ids = getParentTaskIds(cursor);
			getView().expandTasks(ids);
			getView().selectTask(taskId);
		} catch (DbException e) {
			handleError(e);
		}
	}

	private List<Long> getParentTaskIds(Task task) throws DbException {
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