package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Set;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class CollaboratorsTabLogicImpl extends AbstractTabLogicImpl<ICollaboratorsTabLogic.View> implements ICollaboratorsTabLogic {

	@Inject(optional = true)
	private Set<ITabButtonFactory<ICollaboratorsTabLogic>> buttonFactories;
	
	public CollaboratorsTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		CollaboratorsListTableCellProvider contentProvider = new CollaboratorsListTableCellProvider(this, true, false);
		getView().setCollaboratorsProviderCallback(wrapLogicForView(contentProvider, ITableCellProviderCallback.class));

		// Add buttons
		registerButtons(buttonFactories);
	}

	@Override
	public String getLabel() {
		return "Collaborators";
	}
}
