package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;

public class AuthenticationLogicImpl extends AbstractLogicImpl<IAuthenticationLogic.View> implements IAuthenticationLogic {
	
	public AuthenticationLogicImpl(Context context, AbstractLogicImpl<?> parent) {
		super(context, parent);
	}

	@Override
	public void onAuthenticate(String login, String password) {
		try {
			Collaborator collaborator = ModelMgr.getCollaborator(login);
			if (collaborator == null) {
				getRoot().getView().showNotification("Bad user / password.");
			}
			else {
				getContext().setConnectedCollaborator(collaborator);
				getEventBus().fire(new ConnectedCollaboratorEvent(this, collaborator));
			}
		}
		catch (DbException e) {
			handleError(e);
		}
		
	}

}
