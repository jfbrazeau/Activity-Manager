package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;

public abstract class AbstractSafeCallback {
	
	private ILogic<?> source;
	private LogicContext context;

	public AbstractSafeCallback(ILogic<?> source, LogicContext context) {
		this.source = source;
		this.context = context;
	}

	protected void fireCallbackExceptionEvent(Throwable error) {
		context.getEventBus().fire(new CallbackExceptionEvent(source, error));
	}

	protected ILogic<?> getSource() {
		return source;
	}

	protected LogicContext getContext() {
		return context;
	}

}
