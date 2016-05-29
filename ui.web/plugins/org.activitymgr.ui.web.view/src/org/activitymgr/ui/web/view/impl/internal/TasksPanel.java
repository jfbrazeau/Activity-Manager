package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.impl.internal.util.AlignHelper;
import org.activitymgr.ui.web.view.impl.internal.util.TreeTableDatasource;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.TreeTable;

@SuppressWarnings("serial")
public class TasksPanel extends AbstractTabPanel<ITasksTabLogic> implements ITasksTabLogic.View {

	private TreeTable taskTree;

	@Override
	protected Component createBodyComponent() {
		taskTree = new TreeTable();
		addComponent(taskTree);
		taskTree.setImmediate(true);
		taskTree.setMultiSelect(false);
		taskTree.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onTaskSelected(event.getProperty().getValue());
			}
		});
		return taskTree;
	}
	
    @Override
	public void setTreeContentProviderCallback(
			final ITreeContentProviderCallback<Long> tasksProvider) {
		TreeTableDatasource<Long> dataSource = new TreeTableDatasource<Long>(getResourceCache(), tasksProvider);
		taskTree.setContainerDataSource(dataSource);
		int tableWidth = 20;
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			taskTree.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return tasksProvider.getCell((Long) itemId, (String) propertyId);
				}
			});
			int columnWidth = tasksProvider.getColumnWidth(propertyId);
			tableWidth += columnWidth + 10;
			taskTree.setColumnWidth(propertyId, columnWidth);
			taskTree.setColumnAlignment(propertyId, AlignHelper.toVaadinAlign(tasksProvider.getColumnAlign(propertyId)));
		}
		taskTree.setWidth(tableWidth + "px");
	}
    
}
