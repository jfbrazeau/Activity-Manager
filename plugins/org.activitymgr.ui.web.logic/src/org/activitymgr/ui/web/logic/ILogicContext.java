package org.activitymgr.ui.web.logic;

import org.activitymgr.core.beans.Collaborator;

public interface ILogicContext {

	IViewFactory getViewFactory();

	IEventBus getEventBus();

	Collaborator getConnectedCollaborator();

}