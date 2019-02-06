package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.IUILogicContext;

// TODO Inject ?
public class UIContextImpl implements IUILogicContext {

	private Collaborator connectedCollaborator;

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ILogicContext#getConnectedCollaborator()
	 */
	@Override
	public Collaborator getConnectedCollaborator() {
		return connectedCollaborator;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ILogicContext#setConnectedCollaborator(org.activitymgr.core.dto.Collaborator)
	 */
	@Override
	public void setConnectedCollaborator(Collaborator connectedCollaborator) {
		this.connectedCollaborator = connectedCollaborator;
	}
	
	
}