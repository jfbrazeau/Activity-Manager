package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;

import com.google.inject.Injector;

public class ActivityManagerLogic {

	public ActivityManagerLogic(IRootLogic.View rootView, Injector injector) {
		new RootLogicImpl(rootView, injector);
	}

}
