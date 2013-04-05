package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.logic.impl.event.EventBus;

public class Context {

	private IViewFactory viewFactory;
	private EventBus eventBus;
	private Collaborator connectedCollaborator;

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

	public Collaborator getConnectedCollaborator() {
		return connectedCollaborator;
	}

	public void setConnectedCollaborator(Collaborator connectedCollaborator) {
		this.connectedCollaborator = connectedCollaborator;
	}

}
