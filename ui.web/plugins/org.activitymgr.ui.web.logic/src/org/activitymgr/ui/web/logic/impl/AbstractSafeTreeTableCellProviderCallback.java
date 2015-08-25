package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;

public abstract class AbstractSafeTreeTableCellProviderCallback<TYPE> extends AbstractSafeTableCellProviderCallback<TYPE> implements ITreeContentProviderCallback<TYPE> {
	
	public AbstractSafeTreeTableCellProviderCallback(ILogic<?> source) {
		super(source);
	}

	@Override
	public final Collection<TYPE> getChildren(TYPE element) {
		try {
			return unsafeGetChildren(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return Collections.emptyList();
		}
	}

	protected abstract Collection<TYPE> unsafeGetChildren(TYPE element) throws Exception;

	@Override
	public TYPE getParent(TYPE element) {
		try {
			return unsafeGetParent(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return null;
		}
	}
	
	protected abstract TYPE unsafeGetParent(TYPE element) throws Exception;

	@Override
	public final boolean hasChildren(TYPE element) {
		try {
			return unsafeHasChildren(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return false;
		}
	}

	protected abstract boolean unsafeHasChildren(TYPE element) throws Exception;

	@Override
	public final boolean isRoot(TYPE element) {
		try {
			return unsafeIsRoot(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return false;
		}
	}

	protected abstract boolean unsafeIsRoot(TYPE element);

}
