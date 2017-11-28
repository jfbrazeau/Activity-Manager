package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

public class TaskChooserLogic extends
		AbstractTaskChooserLogicImpl<ITaskChooserLogic.View<?>> {

	public TaskChooserLogic(AbstractLogicImpl<?> parent, Long selectedTask,
			ISelectedTaskCallback callback) {
		super(parent, selectedTask, callback);

		// Open the window
		getRoot().getView().openWindow(getView());

		// Update state
		updateUI();
	}

}
