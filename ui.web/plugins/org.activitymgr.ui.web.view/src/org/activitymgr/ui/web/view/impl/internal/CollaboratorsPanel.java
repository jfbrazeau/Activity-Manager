package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;
import org.activitymgr.ui.web.view.impl.internal.util.TextFieldView;

import com.vaadin.event.LayoutEvents;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class CollaboratorsPanel extends VerticalLayout implements ICollaboratorsTabLogic.View {

	@SuppressWarnings("unused")
	private ICollaboratorsTabLogic logic;

	private Table collaboratorsTable;

	private IResourceCache resourceCache;
	
	public CollaboratorsPanel(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
	}

	@Override
	public void registerLogic(ICollaboratorsTabLogic logic) {
		this.logic = logic;
		setSpacing(true);
		setMargin(true);

		collaboratorsTable = new Table();
		addComponent(collaboratorsTable);
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setHeight("500px");
		collaboratorsTable.setSizeFull();
		addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
			@Override
			public void layoutClick(LayoutClickEvent event) {
				if (event.getClickedComponent() instanceof TextFieldView) {
					TextFieldView txtField = (TextFieldView) event.getClickedComponent();
					txtField.onClick();
				}
			}
		});
	}

    @Override
	public void setCollaboratorsProviderCallback(
			final ITableCellProviderCallback<Long> collaboratorsProviderCallback) {
		TableDatasource<Long> dataSource = new TableDatasource<Long>(getResourceCache(), collaboratorsProviderCallback);
		collaboratorsTable.setContainerDataSource(dataSource);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			collaboratorsTable.addGeneratedColumn(propertyId, new Table.ColumnGenerator() {
				@Override
				public Object generateCell(Table source, Object itemId, Object propertyId) {
					return collaboratorsProviderCallback.getCell((Long) itemId, (String) propertyId);
				}
			});
		}
	}
    
	protected IResourceCache getResourceCache() {
		return resourceCache;
	}

}
