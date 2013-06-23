package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;

/**
 * This event is fired when a collaborator has sucessfully logon on the application.
 * 
 * @author Jean-Francois Brazeau
 */
public class ConnectedCollaboratorEvent extends AbstractEvent {

	/** The collaborator that is connected */
	private Collaborator connectedCollaborator;
	
	/**
	 * Default constructor.
	 * @param source the event's source.
	 * @param connectedCollaborator the connected collaborator.
	 */
	public ConnectedCollaboratorEvent(ILogic<?> source, Collaborator connectedCollaborator) {
		super(source);
		this.connectedCollaborator = connectedCollaborator;
	}

	/**
	 * @return the connected collaborator.
	 */
	public Collaborator getConnectedCollaborator() {
		return connectedCollaborator;
	}

}
