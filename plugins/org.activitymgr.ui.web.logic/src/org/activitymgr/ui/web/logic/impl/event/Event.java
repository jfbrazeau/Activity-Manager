package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.ui.web.logic.ILogic;

public abstract class Event {
	
	private ILogic<?> source;
	
	public Event(ILogic<?> source) {
		this.source = source;
	}
	
	public ILogic<?> getSource() {
		return source;
	}

}
