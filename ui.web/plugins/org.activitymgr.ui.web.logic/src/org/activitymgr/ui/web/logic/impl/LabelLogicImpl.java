package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILabelLogic;

public class LabelLogicImpl extends AbstractLogicImpl<ILabelLogic.View> implements ILabelLogic {

	public LabelLogicImpl(AbstractLogicImpl<?> parent, String label) {
		this(parent, label, false);
	}

	public LabelLogicImpl(AbstractLogicImpl<?> parent, String label,
			boolean htmlMode) {
		super(parent);
		getView().setHtmlMode(htmlMode);
		getView().setLabel(label); // TODO internationalization
	}

}
