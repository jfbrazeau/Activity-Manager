package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;

public abstract class AbstractSafeCallback {
	
	private ILogic<?> source;
	private IEventBus eventBus;

	public AbstractSafeCallback(ILogic<?> source, IEventBus eventBus) {
		this.source = source;
		this.eventBus = eventBus;
	}

	protected void fireCallbackExceptionEvent(Throwable error) {
		eventBus.fire(new CallbackExceptionEvent(source, error));
	}

	protected ILogic<?> getSource() {
		return source;
	}

	protected IEventBus getEventBus() {
		return eventBus;
	}

}
