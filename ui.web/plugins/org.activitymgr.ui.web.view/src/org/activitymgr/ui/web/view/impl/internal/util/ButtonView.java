package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.IButtonLogic;
import org.activitymgr.ui.web.logic.IButtonLogic.View;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.ui.Button;

@SuppressWarnings("serial")
public class ButtonView extends Button implements View {

	private IButtonLogic logic;
	private IResourceCache resourceCache;

	public ButtonView(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
		addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				logic.onClick();
			}
		});
	}

	@Override
	public void setIcon(String iconId) {
		setIcon(resourceCache.getResource(iconId + ".gif"));
	}

	@Override
	public void registerLogic(IButtonLogic logic) {
		this.logic = logic;
	}


}
