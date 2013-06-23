package org.activitymgr.ui.web.view.util;

import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILabelLogic.View;

import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class LabelView extends Label implements View {
	
	@Override
	public void registerLogic(ILabelLogic logic) {
		// This view never calls its logic
	}

	@Override
	public void setLabel(String s) {
		setValue(s);
	}

}
