package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IButtonLogic;

public abstract class AbstractSafeButtonLogicImpl extends AbstractLogicImpl<IButtonLogic.View> implements IButtonLogic {
	
	public AbstractSafeButtonLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);
	}

	@Override
	public final void onClick() {
		try {
			unsafeOnClick();
		}
		catch (Throwable t) {
			handleError(t);
		}
	}

	public abstract void unsafeOnClick() throws Exception;
}
