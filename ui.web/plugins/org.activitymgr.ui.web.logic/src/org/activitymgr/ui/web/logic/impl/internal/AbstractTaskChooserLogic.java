package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

public abstract class AbstractTaskChooserLogic extends
		AbstractTaskChooserLogicImpl<ITaskChooserLogic.View<?>> {

	public AbstractTaskChooserLogic(AbstractLogicImpl<?> parent, Long selectedTask) {
		super(parent, selectedTask);

		// Open the window
		getRoot().getView().openWindow(getView());

		// Update state
		updateUI();
	}

}
