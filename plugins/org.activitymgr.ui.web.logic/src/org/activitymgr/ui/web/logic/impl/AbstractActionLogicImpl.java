package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IActionLogic;

public abstract class AbstractActionLogicImpl extends AbstractLogicImpl<IActionLogic.View> implements IActionLogic {

	public AbstractActionLogicImpl(AbstractLogicImpl<?> parent, String label) {
		super(parent);
		getView().setLabel(label);
	}

}
