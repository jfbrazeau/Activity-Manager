package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Set;

import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.TaskSelectedEvent;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class TasksTabLogicImpl extends AbstractTabLogicImpl<ITasksTabLogic.View> implements ITasksTabLogic {

	@Inject(optional = true)
	private Set<ITabButtonFactory<ITasksTabLogic>> buttonFactories;
	private TaskTreeCellProvider treeContentCallback;
	
	public TasksTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		treeContentCallback = new TaskTreeCellProvider(this, null, false);
		getView().setTreeContentProviderCallback(wrapLogicForView(treeContentCallback, ITreeContentProviderCallback.class));

		// Add buttons
		registerButtons(buttonFactories);
	}

	@Override
	public String getLabel() {
		return "Tasks";
	}

	@Override
	public void onTaskSelected(Object value) {
		getEventBus().fire(new TaskSelectedEvent(this, (Long) value));
	}

	@Override
	public void dispose() {
		treeContentCallback.dispose();
		super.dispose();
	}
}
