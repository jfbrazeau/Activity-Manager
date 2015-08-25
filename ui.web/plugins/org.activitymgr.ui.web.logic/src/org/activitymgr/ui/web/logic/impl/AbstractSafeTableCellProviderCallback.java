package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;

public abstract class AbstractSafeTableCellProviderCallback<TYPE> extends AbstractSafeCallback implements ITableCellProviderCallback<TYPE> {
	
	public AbstractSafeTableCellProviderCallback(ILogic<?> source) {
		super(source);
	}

	@Override
	public final Collection<TYPE> getRootElements() {
		try {
			return unsafeGetRootElements();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return Collections.emptyList();
		}
	}

	protected abstract Collection<TYPE> unsafeGetRootElements() throws Exception;
	
	@Override
	public final boolean contains(TYPE element) {
		try {
			return unsafeContains(element);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return false;
		}
	}

	protected abstract boolean unsafeContains(TYPE element) throws Exception;

	@Override
	public final IView<?> getCell(TYPE itemId, String propertyId) {
		try {
			return unsafeGetCell(itemId, propertyId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
			return null;
		}
	}
	
	protected abstract IView<?> unsafeGetCell(TYPE itemId, String propertyId) throws Exception;

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
