package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;

public class LogoutEvent extends AbstractEvent {

	public LogoutEvent(ILogic<?> source) {
		super(source);
	}

}