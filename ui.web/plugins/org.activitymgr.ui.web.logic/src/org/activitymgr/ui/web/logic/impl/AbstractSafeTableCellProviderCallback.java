package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;

public abstract class AbstractSafeTableCellProviderCallback<ITEM_ID_TYPE> extends AbstractSafeCallback implements ITableCellProviderCallback<ITEM_ID_TYPE> {
	
	public AbstractSafeTableCellProviderCallback(ILogic<?> source) {
		super(source);
	}

	@Override
	public final Collection<ITEM_ID_TYPE> getRootElements() {
		try {
			return unsafeGetRootElements();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return Collections.emptyList();
		}
	}

	protected abstract Collection<ITEM_ID_TYPE> unsafeGetRootElements() throws Exception;
	
	@Override
	public final boolean contains(ITEM_ID_TYPE element) {
		try {
			return unsafeContains(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return false;
		}
	}

	protected abstract boolean unsafeContains(ITEM_ID_TYPE element) throws Exception;

	@Override
	public final IView<?> getCell(ITEM_ID_TYPE itemId, String propertyId) {
		try {
			return unsafeGetCell(itemId, propertyId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return null;
		}
	}
	
	protected abstract IView<?> unsafeGetCell(ITEM_ID_TYPE itemId, String propertyId) throws Exception;

	@Override
	public final Collection<String> getPropertyIds() {
		try {
			return unsafeGetPropertyIds();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return Collections.emptyList();
		}
	}

	protected abstract Collection<String> unsafeGetPropertyIds() throws Exception;

	@Override
	public final Integer getColumnWidth(String propertyId) {
		try {
			return unsafeGetColumnWidth(propertyId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return null;
		}
	}

	protected Integer unsafeGetColumnWidth(String propertyId) {
		return null;
	}

	@Override
	public final String getFooter(String propertyId) {
		try {
			return unsafeGetFooter(propertyId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return null;
		}
	}

	protected String unsafeGetFooter(String propertyId) {
		return null;
	}

}
