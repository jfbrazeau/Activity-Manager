package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;

public class ActivityManagerLogic {

	public ActivityManagerLogic(IViewFactory viewFactory) {
		new RootLogicImpl(viewFactory);
	}

}
