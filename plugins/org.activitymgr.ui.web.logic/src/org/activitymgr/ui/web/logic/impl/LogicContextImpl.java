package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.IViewFactory;

public class LogicContextImpl implements ILogicContext {

	private IViewFactory viewFactory;
	private IEventBus eventBus;
	private Collaborator connectedCollaborator;

	public LogicContextImpl(IViewFactory viewFactory, IEventBus eventBus) {
		this.viewFactory = viewFactory;
		this.eventBus = eventBus;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.ILogicContext#getViewFactory()
	 */
	@Override
	public IViewFactory getViewFactory() {
		return viewFactory;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.ILogicContext#getEventBus()
	 */
	@Override
	public IEventBus getEventBus() {
		return eventBus;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.ILogicContext#getConnectedCollaborator()
	 */
	@Override
	public Collaborator getConnectedCollaborator() {
		return connectedCollaborator;
	}

	public void setConnectedCollaborator(Collaborator connectedCollaborator) {
		this.connectedCollaborator = connectedCollaborator;
	}

}
