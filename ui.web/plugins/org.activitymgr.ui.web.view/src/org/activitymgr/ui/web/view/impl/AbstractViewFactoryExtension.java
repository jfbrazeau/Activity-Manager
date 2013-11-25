package org.activitymgr.ui.web.view.impl;

import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.view.IResourceCache;

public abstract class AbstractViewFactoryExtension implements IViewFactory {

	private IResourceCache resourceCache;
	private IRootLogic.View rootView;

	public final void initialize(IRootLogic.View rootView, IResourceCache resourceCache) {
		this.rootView = rootView;
		this.resourceCache = resourceCache;
	}

	protected IResourceCache getResourceCache() {
		return resourceCache;
	}

	protected IRootLogic.View getRootView() {
		return rootView;
	}

}
