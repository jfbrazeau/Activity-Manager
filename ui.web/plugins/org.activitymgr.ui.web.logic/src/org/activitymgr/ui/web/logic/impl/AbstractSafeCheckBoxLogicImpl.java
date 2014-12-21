package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;

public abstract class AbstractSafeCheckBoxLogicImpl extends AbstractLogicImpl<ICheckBoxFieldLogic.View> implements ICheckBoxFieldLogic {

	public AbstractSafeCheckBoxLogicImpl(AbstractLogicImpl<?> parent, boolean value) {
		super(parent);
		getView().setValue(value);
	}
	
	@Override
	public final void onValueChanged(boolean newValue) {
		try {
			unsafeOnValueChanged(newValue);
		}
		catch (Throwable t) {
			getView().focus();
			getEventBus().fire(new CallbackExceptionEvent(this, t));
		}
	}

	protected abstract void unsafeOnValueChanged(boolean newValue) throws Exception;

}
