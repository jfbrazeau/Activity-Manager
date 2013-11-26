package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.LogicContext;

public class TableFolderLogicImpl extends AbstractLogicImpl<ITabFolderLogic.View> implements ITabFolderLogic {

	public TableFolderLogicImpl(ILogic<?> parent, LogicContext context) {
		super(parent, context);
	}

	public void addTab(String label, ILogic<?> logic) {
		getView().addTab(label, logic.getView());
	}

}
