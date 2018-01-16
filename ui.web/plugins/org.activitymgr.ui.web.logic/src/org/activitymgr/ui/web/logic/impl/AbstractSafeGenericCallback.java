package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IGenericCallback;

public abstract class AbstractSafeGenericCallback<RESULT> extends AbstractSafeCallback implements IGenericCallback<RESULT> {

	public AbstractSafeGenericCallback(AbstractLogicImpl<?> source) {
		super(source);
	}

	@Override
	public void callback(RESULT result) {
		try {
			unsafeCallback(result);
		}
		catch (Throwable t) {
			doThrow(t);
		}
	}

	protected abstract void unsafeCallback(RESULT result) throws Exception;

}
