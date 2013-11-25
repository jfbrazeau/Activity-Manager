package org.activitymgr.ui.web.view;

import org.activitymgr.ui.web.logic.ILabelProviderCallback.Icon;

import com.vaadin.server.Resource;

public interface IResourceCache {
	
	Resource getResource(String path);

	Resource getIconResource(Icon icon);

}
