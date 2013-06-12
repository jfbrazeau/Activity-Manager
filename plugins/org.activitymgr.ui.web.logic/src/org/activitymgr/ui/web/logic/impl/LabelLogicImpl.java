package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILogic;

public class LabelLogicImpl extends AbstractLogicImpl<ILabelLogic.View> implements ILabelLogic {

	public LabelLogicImpl(ILogic<?> parent, String label) {
		super(parent);
		getView().setLabel(label); // TODO internationalization
	}

}
