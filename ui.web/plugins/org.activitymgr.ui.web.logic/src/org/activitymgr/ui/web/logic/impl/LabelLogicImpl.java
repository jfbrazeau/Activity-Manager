package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

public class LabelLogicImpl extends AbstractLogicImpl<ILabelLogic.View> implements ILabelLogic {

	public LabelLogicImpl(AbstractLogicImpl<?> parent, String label) {
		super(parent);
		getView().setLabel(label); // TODO internationalization
	}

}
