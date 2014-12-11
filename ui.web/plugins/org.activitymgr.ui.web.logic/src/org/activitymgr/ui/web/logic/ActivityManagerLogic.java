package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;

public class ActivityManagerLogic {

	public ActivityManagerLogic(IRootLogic.View rootView, IViewDescriptor viewDescriptor) {
		new RootLogicImpl(rootView, viewDescriptor);
	}

}
