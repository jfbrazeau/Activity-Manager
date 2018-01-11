package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITabLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

public class TabFolderLogicImpl extends AbstractLogicImpl<ITabFolderLogic.View> implements ITabFolderLogic {
	
	public static final String SELECTED_TAB_COOKIE = "selectedTab";

	public TabFolderLogicImpl(ILogic<?> parent) {
		super(parent);
	}

	public void addTab(String id, String label, ITabLogic<?> logic) {
		getView().addTab(id, label, logic.getView());
	}

	@Override
	public void onSelectedTabChanged(String id) {
		getRoot().getView().setCookie(SELECTED_TAB_COOKIE, id);
	}

	public void setSelectedTab(String id) {
		getView().setSelectedTab(id);
	}

}
