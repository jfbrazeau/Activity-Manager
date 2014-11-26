package org.activitymgr.ui.web.view.impl.internal;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.BasicTreeDatasource;

import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class TasksPanel extends VerticalLayout implements ITasksTabLogic.View {

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
		taskTree.addContainerProperty(ITreeContentProviderCallback.NAME_PROPERTY_ID, String.class, null);
		//taskTree.setColumnHeader(ITreeContentProviderCallback.NAME_PROPERTY_ID, "Task");
	}

    @Override
	public void setTreeContentProviderCallback(
			ITreeContentProviderCallback<?> treeContentProviderCallback) {
		BasicTreeDatasource dataSource = new BasicTreeDatasource(getResourceCache(), treeContentProviderCallback);
		taskTree.setContainerDataSource(dataSource);
		taskTree.setItemCaptionPropertyId(ILabelProviderCallback.NAME_PROPERTY_ID);
	}
    
    protected IResourceCache getResourceCache() {
		return resourceCache;
	}

}
