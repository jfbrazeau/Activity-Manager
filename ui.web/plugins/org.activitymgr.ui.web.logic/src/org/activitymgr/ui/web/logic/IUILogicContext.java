package org.activitymgr.ui.web.logic;

import org.activitymgr.core.dto.Collaborator;

public interface IUILogicContext {

	Collaborator getConnectedCollaborator();

	void setConnectedCollaborator(
			Collaborator connectedCollaborator);

}