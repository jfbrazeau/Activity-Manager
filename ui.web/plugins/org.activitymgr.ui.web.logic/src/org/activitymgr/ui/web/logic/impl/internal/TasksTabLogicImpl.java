package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

public class TasksTabLogicImpl extends AbstractLogicImpl<ITasksTabLogic.View> implements ITasksTabLogic {

	public TasksTabLogicImpl(AbstractLogicImpl<ITasksTabLogic.View> parent) {
		super(parent);

		TaskTreeContentProvider treeContentCallback = new TaskTreeContentProvider(this, getContext(), getModelMgr());
		getView().setTreeContentProviderCallback(getContext().buildTransactionalWrapper(treeContentCallback, ITreeContentProviderCallback.class));
	}

}
