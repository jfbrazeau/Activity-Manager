package org.activitymgr.ui.web.logic.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Task;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;

public class TaskChooserLogicImpl extends AbstractLogicImpl<ITaskChooserLogic.View> implements ITaskChooserLogic {
	
	private List<Long> selectedTaskIds;

	public TaskChooserLogicImpl(ILogic<?> parent, List<Long> selectedTaskIds) {
		super(parent);
		// Remember already selected task ids
		this.selectedTaskIds = selectedTaskIds;
		// Register the tree content provider
		getView().setTreeContentProviderCallback(new TaskTreeContentProvider(this, getEventBus()));
		// Update button state & status label
		onSelectionChanged(null);
		
	}

	@Override
	public void onSelectionChanged(Long taskId) {
		try {
			String newStatus = "";
			boolean okButtonEnabled = false;
			if (taskId != null) {
				if (selectedTaskIds.contains(taskId)) {
					newStatus = "This task is already selected";
				} else {
					Task task = ModelMgr.getTask(taskId);
					if (task.getSubTasksCount() != 0) {
						newStatus = "You cannot select a container task";
					}
					else {
						okButtonEnabled = true;
					}
				}
			}
			getView().setStatus(newStatus);
			getView().setOkButtonEnabled(okButtonEnabled);
		}
		catch (DbException e) {
			handleError(e);
		}
	}

	@Override
	public void onTaskChosen(long taskId) {
		((ContributionsLogicImpl) getParent()).addTask(taskId);
	}

}

class TaskTreeContentProvider extends AbstractSafeTreeContentProviderCallback {

	public TaskTreeContentProvider(ILogic<?> source, IEventBus eventBus) {
		super(source, eventBus);
	}

	@Override
	protected ILabelProviderCallback unsafeGetLabelProvider(final String itemId)
			throws Exception {
		return new AbstractSafeLabelProviderCallback(getSource(), getEventBus()) {
			
			@Override
			protected String unsafeGetText() throws Exception {
				return ModelMgr.getTask(Long.parseLong(itemId)).getName();
			}
			
			@Override
			protected Icon unsafeGetIcon() throws Exception {
				return null;
			}
		};
	}

	@Override
	protected Collection<String> unsafeGetChildren(String itemId)
			throws Exception {
		Task[] subTasks = ModelMgr.getSubtasks(itemId == null ? null : Long.parseLong(itemId));
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