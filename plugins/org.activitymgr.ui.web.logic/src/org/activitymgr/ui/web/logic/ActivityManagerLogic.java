package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.impl.LogicContextImpl;
import org.activitymgr.ui.web.logic.impl.RootLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.EventBusImpl;

public class ActivityManagerLogic {

	public ActivityManagerLogic(IViewFactory viewFactory) {
		new RootLogicImpl(new LogicContextImpl(viewFactory, new EventBusImpl()));
	}

}
