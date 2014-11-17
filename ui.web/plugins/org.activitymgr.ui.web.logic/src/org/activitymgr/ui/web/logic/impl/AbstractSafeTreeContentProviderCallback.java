package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;

public abstract class AbstractSafeTreeContentProviderCallback extends AbstractSafeListContentProviderCallback implements ITreeContentProviderCallback {
	
	public AbstractSafeTreeContentProviderCallback(ILogic<?> source, IEventBus eventBus) {
		super(source, eventBus);
	}

	@Override
	public final Collection<String> getChildren(String itemId) {
		try {
			return unsafeGetChildren(itemId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return Collections.emptyList();
	}

	protected abstract Collection<String> unsafeGetChildren(String itemId) throws Exception;

	@Override
	public final boolean isRoot(String itemId) {
		try {
			return unsafeIsRoot(itemId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return false;
	}

	protected abstract boolean unsafeIsRoot(String itemId) throws Exception;

}
