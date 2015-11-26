package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;

public abstract class AbstractSafeTreeTableCellProviderCallback<ITEM_ID_TYPE> extends AbstractSafeTableCellProviderCallback<ITEM_ID_TYPE> implements ITreeContentProviderCallback<ITEM_ID_TYPE> {
	
	public AbstractSafeTreeTableCellProviderCallback(ILogic<?> source) {
		super(source);
	}

	@Override
	public final Collection<ITEM_ID_TYPE> getChildren(ITEM_ID_TYPE element) {
		try {
			return unsafeGetChildren(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return Collections.emptyList();
		}
	}

	protected abstract Collection<ITEM_ID_TYPE> unsafeGetChildren(ITEM_ID_TYPE element) throws Exception;

	@Override
	public ITEM_ID_TYPE getParent(ITEM_ID_TYPE element) {
		try {
			return unsafeGetParent(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return null;
		}
	}
	
	protected abstract ITEM_ID_TYPE unsafeGetParent(ITEM_ID_TYPE element) throws Exception;

	@Override
	public final boolean hasChildren(ITEM_ID_TYPE element) {
		try {
			return unsafeHasChildren(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return false;
		}
	}

	protected abstract boolean unsafeHasChildren(ITEM_ID_TYPE element) throws Exception;

	@Override
	public final boolean isRoot(ITEM_ID_TYPE element) {
		try {
			return unsafeIsRoot(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return false;
		}
	}

	protected abstract boolean unsafeIsRoot(ITEM_ID_TYPE element);

}
