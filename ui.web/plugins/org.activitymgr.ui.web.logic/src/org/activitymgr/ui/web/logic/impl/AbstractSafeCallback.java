package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILogicContext;

import com.google.inject.Inject;

public abstract class AbstractSafeCallback {
	
	private AbstractLogicImpl<?> source;
	
	@Inject
	private ILogicContext context;

	public AbstractSafeCallback(AbstractLogicImpl<?> source) {
		this.source = source;
		source.injectMembers(this);
	}

	protected AbstractLogicImpl<?> getSource() {
		return source;
	}

	protected ILogicContext getContext() {
		return context;
	}

	protected void doThrow(Throwable t) {
		if (t instanceof Error) {
			throw (Error) t;
		}
		else if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		else {
			throw new IllegalStateException(t);
		}
	}

}
