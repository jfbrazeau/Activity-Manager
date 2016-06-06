package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.impl.internal.util.AlignHelper;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;

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
		collaboratorsTable.setSizeFull();
		return collaboratorsTable;
	}
    @Override
	public void setCollaboratorsProviderCallback(
			final ITableCellProviderCallback<Long> collaboratorsProvider) {
		TableDatasource<Long> dataSource = new TableDatasource<Long>(getResourceCache(), collaboratorsProvider);
		collaboratorsTable.setContainerDataSource(dataSource);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			collaboratorsTable.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return collaboratorsProvider.getCell((Long) itemId, (String) propertyId);
				}
			});
			int columnWidth = collaboratorsProvider.getColumnWidth(propertyId);
			collaboratorsTable.setColumnWidth(propertyId, columnWidth);
			collaboratorsTable.setColumnAlignment(propertyId, AlignHelper.toVaadinAlign(collaboratorsProvider.getColumnAlign(propertyId)));
		}
	}
    
}
