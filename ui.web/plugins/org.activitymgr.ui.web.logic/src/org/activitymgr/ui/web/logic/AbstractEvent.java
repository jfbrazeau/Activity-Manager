package org.activitymgr.ui.web.logic;


public abstract class AbstractEvent {
	
	private ILogic<?> source;
	
	public AbstractEvent(ILogic<?> source) {
		this.source = source;
	}
	
	public ILogic<?> getSource() {
		return source;
	}

}
