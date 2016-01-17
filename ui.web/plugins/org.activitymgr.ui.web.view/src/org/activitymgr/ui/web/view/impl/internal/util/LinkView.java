package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.ILinkLogic;
import org.activitymgr.ui.web.logic.ILinkLogic.View;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Link;

@SuppressWarnings("serial")
public class LinkView extends Link implements View {
	
	public LinkView() {
		setSizeUndefined(); // This disables text wrapping
		setTargetName("_blank");
	}

	@Override
	public void registerLogic(ILinkLogic logic) {
		// This view never calls its logic
	}

	@Override
	public void setLabel(String s) {
		setCaption(s);
	}

	@Override
	public void setHref(String href) {
		setResource(new ExternalResource(href));
	}

}
