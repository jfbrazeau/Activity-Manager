package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.logic.impl.event.EventBus;

public class Context {

	private IViewFactory viewFactory;
	private EventBus eventBus;

	public Context(IViewFactory viewFactory, EventBus eventBus) {
		this.viewFactory = viewFactory;
		this.eventBus = eventBus;
	}

	public IViewFactory getViewFactory() {
		return viewFactory;
	}

	public EventBus getEventBus() {
		return eventBus;
	}

}
