package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;

public abstract class AbstractSafeCallback {
	
	private AbstractLogicImpl<?> callbackProvider;

	public AbstractSafeCallback(AbstractLogicImpl<?> callbackProvider) {
		this.callbackProvider = callbackProvider;
	}

	protected void fireCallbackExceptionEvent(Throwable error) {
		callbackProvider.getEventBus().fire(new CallbackExceptionEvent(callbackProvider, error));
	}

	protected AbstractLogicImpl<?> getCallbackProvider() {
		return callbackProvider;
	}

}
