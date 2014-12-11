package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.BasicListDatasource;

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
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setHeight("500px");
		collaboratorsTable.setSizeFull();
	}

    @Override
	public void setCollaboratorsProviderCallback(
			IListContentProviderCallback<Long> collaboratorsProviderCallback) {
		BasicListDatasource dataSource = new BasicListDatasource(getResourceCache(), collaboratorsProviderCallback);
		collaboratorsTable.setContainerDataSource(dataSource);
	}
    
	protected IResourceCache getResourceCache() {
		return resourceCache;
	}

}
