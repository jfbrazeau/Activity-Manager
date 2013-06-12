package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;

public abstract class AbstractTextFieldLogicImpl extends AbstractLogicImpl<ITextFieldLogic.View> implements ITextFieldLogic {

	public AbstractTextFieldLogicImpl(ILogic<?> parent, String value) {
		super(parent);
		getView().setValue(value);
	}

}
