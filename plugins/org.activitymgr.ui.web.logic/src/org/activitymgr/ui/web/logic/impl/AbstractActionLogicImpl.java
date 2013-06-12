package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IActionLogic;
import org.activitymgr.ui.web.logic.ILogic;

public abstract class AbstractActionLogicImpl extends AbstractLogicImpl<IActionLogic.View> implements IActionLogic {

	public AbstractActionLogicImpl(ILogic<?> parent, String label) {
		super(parent);
		getView().setLabel(label);
	}

}
