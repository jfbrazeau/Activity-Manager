package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;

public class CollaboratorsTabLogicImpl extends AbstractTabLogicImpl<ICollaboratorsTabLogic.View> implements ICollaboratorsTabLogic {

	public CollaboratorsTabLogicImpl(AbstractLogicImpl<ICollaboratorsTabLogic.View> parent) {
		super(parent);
		CollaboratorsListTableCellProvider contentProvider = new CollaboratorsListTableCellProvider(this, true, false);
		getView().setCollaboratorsProviderCallback(buildTransactionalWrapper(contentProvider, ITableCellProviderCallback.class));
	}

}
