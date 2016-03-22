package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic;

public abstract class AbstractSafeCheckBoxLogicImpl extends AbstractLogicImpl<ICheckBoxFieldLogic.View> implements ICheckBoxFieldLogic {

	public AbstractSafeCheckBoxLogicImpl(AbstractLogicImpl<?> parent, boolean value) {
		super(parent);
		getView().setValue(value);
	}
	
	@Override
	public final void onValueChanged(Boolean newValue) {
		try {
			unsafeOnValueChanged(newValue);
		}
		catch (Throwable t) {
			getView().focus();
			doThrow(t);
		}
	}

	protected abstract void unsafeOnValueChanged(Boolean newValue) throws Exception;

}
