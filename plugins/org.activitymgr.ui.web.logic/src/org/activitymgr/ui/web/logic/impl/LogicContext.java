package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IViewFactory;

public class LogicContext {

	private IViewFactory viewFactory;
	private IEventBus eventBus;
	private Collaborator connectedCollaborator;
	private ModelMgr modelMgr;

	public LogicContext(IViewFactory viewFactory, IEventBus eventBus, ModelMgr modelMgr) {
		this.viewFactory = viewFactory;
		this.eventBus = eventBus;
		this.modelMgr = modelMgr;
	}

	public ModelMgr getModelMgr() {
		return modelMgr;
	}
	
	public IViewFactory getViewFactory() {
		return viewFactory;
	}

	public IEventBus getEventBus() {
		return eventBus;
	}

	public Collaborator getConnectedCollaborator() {
		return connectedCollaborator;
	}

	public void setConnectedCollaborator(Collaborator connectedCollaborator) {
		this.connectedCollaborator = connectedCollaborator;
	}

}
