package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

public class TabFolderLogicImpl extends AbstractLogicImpl<ITabFolderLogic.View> implements ITabFolderLogic {

	public TabFolderLogicImpl(ILogic<?> parent) {
		super(parent);
	}

	public void addTab(String label, ILogic<?> logic) {
		getView().addTab(label, logic.getView());
	}

}
