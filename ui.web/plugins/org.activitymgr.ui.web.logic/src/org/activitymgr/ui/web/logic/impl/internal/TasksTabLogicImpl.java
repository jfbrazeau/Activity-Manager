package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Set;

import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class TasksTabLogicImpl extends AbstractTabLogicImpl<ITasksTabLogic.View> implements ITasksTabLogic {

	@Inject(optional = true)
	private Set<ITabButtonFactory<ITasksTabLogic>> buttonFactories;
	
	public TasksTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		TaskTreeContentProvider treeContentCallback = new TaskTreeContentProvider(this, null);
		getView().setTreeContentProviderCallback(buildTransactionalWrapper(treeContentCallback, ITreeContentProviderCallback.class));

		// Add buttons
		registerButtons(buttonFactories);
	}

	@Override
	public String getLabel() {
		return "Tasks";
	}
}
