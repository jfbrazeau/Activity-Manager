package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.impl.Context;
import org.activitymgr.ui.web.logic.impl.RootLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.EventBus;

public class ActivityManagerLogic {

	public ActivityManagerLogic(IViewFactory viewFactory) {
		new RootLogicImpl(new Context(viewFactory, new EventBus()));
	}

}
