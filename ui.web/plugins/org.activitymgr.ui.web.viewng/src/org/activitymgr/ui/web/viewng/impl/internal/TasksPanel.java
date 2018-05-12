package org.activitymgr.ui.web.viewng.impl.internal;

import org.activitymgr.ui.web.logic.IButtonLogic.View;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.viewng.AbstractTabPanel;
import org.activitymgr.ui.web.viewng.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class TasksPanel extends AbstractTabPanel<ITasksTabLogic> implements
		ITasksTabLogic.View {

	@Inject
	public TasksPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	public void addButton(View<?> buttonView) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerLogic(ITasksTabLogic logic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTreeContentProviderCallback(
			ITreeContentProviderCallback<Long> tasksProviderCallback) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Component createBodyComponent() {
		return new Label("Fake tasks panel");
	}

}
