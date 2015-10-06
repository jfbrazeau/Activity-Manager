package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;

import com.google.inject.Inject;

public abstract class AbstractSafeCallback {
	
	private ILogic<?> source;
	
	@Inject
	private ILogicContext context;

	@Inject
	private IEventBus eventBus;

	public AbstractSafeCallback(ILogic<?> source) {
		this.source = source;
		source.injectMembers(this);
	}

	protected void fireCallbackExceptionEvent(Throwable error) {
		eventBus.fire(new CallbackExceptionEvent(source, error));
	}

	protected ILogic<?> getSource() {
		return source;
	}

	protected ILogicContext getContext() {
		return context;
	}

}
