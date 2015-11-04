package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILabelLogic.View.Align;

public class LabelLogicImpl extends AbstractLogicImpl<ILabelLogic.View> implements ILabelLogic {

	public LabelLogicImpl(AbstractLogicImpl<?> parent, String label) {
		this(parent, label, Align.LEFT);
	}

	public LabelLogicImpl(AbstractLogicImpl<?> parent, String label, View.Align align) {
		super(parent);
		getView().setLabel(label); // TODO internationalization
		getView().setAlign(align);
	}

}
