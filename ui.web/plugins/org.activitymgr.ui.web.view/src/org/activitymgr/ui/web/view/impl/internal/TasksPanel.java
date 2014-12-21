package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.TreeTableDatasource;

import com.vaadin.ui.Table;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class TasksPanel extends VerticalLayout implements ITasksTabLogic.View {

	@SuppressWarnings("unused")
	private ITasksTabLogic logic;

	private TreeTable taskTree;

	private IResourceCache resourceCache;

	public TasksPanel(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
	}

	@Override
	public void registerLogic(ITasksTabLogic logic) {
		this.logic = logic;
		setSpacing(true);
		setMargin(true);

		taskTree = new TreeTable();
		addComponent(taskTree);
		taskTree.setImmediate(true);
		taskTree.setSizeFull();
	}

    @Override
	public void setTreeContentProviderCallback(
			final ITreeContentProviderCallback<Long> tasksProviderCallback) {
		TreeTableDatasource<Long> dataSource = new TreeTableDatasource<Long>(getResourceCache(), tasksProviderCallback);
		taskTree.setContainerDataSource(dataSource);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			taskTree.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return tasksProviderCallback.getCell((Long) itemId, (String) propertyId);
				}
			});
		}
	}
    
	protected IResourceCache getResourceCache() {
		return resourceCache;
	}

}
