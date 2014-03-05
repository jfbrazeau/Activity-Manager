package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.DbException;
import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.beans.Task;
import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeLabelProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.LogicContext;

public class TaskChooserLogicImpl extends AbstractLogicImpl<ITaskChooserLogic.View> implements ITaskChooserLogic {
	
	private List<Long> selectedTaskIds;

	public TaskChooserLogicImpl(AbstractLogicImpl<?> parent, List<Long> selectedTaskIds) {
		super(parent);
		// Remember already selected task ids
		this.selectedTaskIds = selectedTaskIds;
		// Register the tree content provider
		TaskTreeContentProvider callback = new TaskTreeContentProvider(this, getContext(), getModelMgr());
		getView().setTreeContentProviderCallback(getContext().buildTransactionalWrapper(callback, ITreeContentProviderCallback.class));
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
					if (!getModelMgr().isLeaf(taskId)) {
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