package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.BasicTreeDatasource;

import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class TasksPanel extends VerticalLayout implements ITasksTabLogic.View {

	@SuppressWarnings("unused")
	private ITasksTabLogic logic;

	private IResourceCache resourceCache;

	private TreeTable taskTree;

	public TasksPanel(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
	}

	@Override
	public void registerLogic(ITasksTabLogic logic) {
		this.logic = logic;
		setSpacing(true);
		setMargin(true);

		taskTree = new TreeTable();
		taskTree.setSizeFull();
		addComponent(taskTree);
		taskTree.setImmediate(true);
	}

    @Override
	public void setTreeContentProviderCallback(
			ITreeContentProviderCallback<Long> tasksProviderCallback) {
		BasicTreeDatasource dataSource = new BasicTreeDatasource(getResourceCache(), tasksProviderCallback);
		taskTree.setContainerDataSource(dataSource);
	}
    
    protected IResourceCache getResourceCache() {
		return resourceCache;
	}

}
