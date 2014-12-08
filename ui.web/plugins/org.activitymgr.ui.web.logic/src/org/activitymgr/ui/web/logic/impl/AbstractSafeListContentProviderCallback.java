package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;

public abstract class AbstractSafeListContentProviderCallback<TYPE> extends AbstractSafeLabelProviderCallback<TYPE> implements IListContentProviderCallback<TYPE> {
	
	public AbstractSafeListContentProviderCallback(ILogic<?> source, IEventBus eventBus) {
		super(source, eventBus);
	}

	@Override
	public final Collection<TYPE> getRootElements() {
		try {
			return unsafeGetRootElements();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return Collections.emptyList();
	}

	protected abstract Collection<TYPE> unsafeGetRootElements() throws Exception;
	
	@Override
	public final boolean contains(TYPE element) {
		try {
			return unsafeContains(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return false;
	}

	protected abstract boolean unsafeContains(TYPE element);

}
