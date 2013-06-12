package org.activitymgr.ui.web.view.util;

import org.activitymgr.ui.web.logic.IActionLogic;

import com.vaadin.ui.Button;

@SuppressWarnings("serial")
public class ActionView extends Button implements IActionLogic.View {
	
	private IActionLogic logic;
	
	@Override
	public void registerLogic(IActionLogic newLogic) {
		this.logic = newLogic;
		addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				logic.onActionInvoked();
			}
		});
	}

	@Override
	public void setLabel(String label) {
		setCaption(label);
	}
	
}
