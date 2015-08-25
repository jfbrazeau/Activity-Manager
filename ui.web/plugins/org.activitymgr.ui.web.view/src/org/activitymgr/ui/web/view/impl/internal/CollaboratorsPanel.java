package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;
import org.activitymgr.ui.web.view.impl.internal.util.TextFieldView;

import com.vaadin.event.LayoutEvents;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;

@SuppressWarnings("serial")
public class CollaboratorsPanel extends AbstractTabPanel<ICollaboratorsTabLogic> implements ICollaboratorsTabLogic.View {

	private Table collaboratorsTable;

	@Override
	protected Component createBodyComponent() {
		// Collaborators table
		collaboratorsTable = new Table();
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setHeight("500px");
//		/collaboratorsTable.setSizeFull();
		addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
			@Override
			public void layoutClick(LayoutClickEvent event) {
				if (event.getClickedComponent() instanceof TextFieldView) {
					TextFieldView txtField = (TextFieldView) event.getClickedComponent();
					txtField.onClick();
				}
			}
		});
		return collaboratorsTable;
	}
    @Override
	public void setCollaboratorsProviderCallback(
			final ITableCellProviderCallback<Long> collaboratorsProvider) {
		TableDatasource<Long> dataSource = new TableDatasource<Long>(getResourceCache(), collaboratorsProvider);
		collaboratorsTable.setContainerDataSource(dataSource);
		int tableWidth = 20;
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			collaboratorsTable.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return collaboratorsProvider.getCell((Long) itemId, (String) propertyId);
				}
			});
			int columnWidth = collaboratorsProvider.getColumnWidth(propertyId);
			tableWidth += columnWidth + 10;
			collaboratorsTable.setColumnWidth(propertyId, columnWidth);
		}
		collaboratorsTable.setWidth(tableWidth + "px");
	}
    
}
