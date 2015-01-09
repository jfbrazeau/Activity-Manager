package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;

public abstract class AbstractSafeTextFieldLogicImpl extends AbstractLogicImpl<ITextFieldLogic.View> implements ITextFieldLogic {

	private String lastValue = null;
	private boolean editOnClick;
	
	public AbstractSafeTextFieldLogicImpl(AbstractLogicImpl<?> parent, String value, boolean editOnClick) {
		super(parent);
		this.editOnClick = editOnClick;
		getView().setValue(value);
		lastValue = value;
		if (editOnClick) {
			getView().setReadOnly(true);
		}
	}
	
	@Override
	public void onEnterKeyPressed() {
		if (editOnClick) {
			getView().blur();
		}
	}

	@Override
	public void onClick() {
		if (editOnClick) {
			getView().setReadOnly(false);
			getView().focus();
		}
	}

	@Override
	public final void onValueChanged(String newValue) {
		try {
			if (lastValue == null || !lastValue.equals(newValue)) {
				unsafeOnValueChanged(newValue);
				lastValue = newValue;
			}
			if (editOnClick) {
				getView().setReadOnly(true);
			}
		}
		catch (Throwable t) {
			getView().selectAll();
			getView().focus();
			getEventBus().fire(new CallbackExceptionEvent(this, t));
		}
	}

	protected abstract void unsafeOnValueChanged(String newValue) throws Exception;

}
