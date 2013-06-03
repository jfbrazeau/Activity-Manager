package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.IAuthenticatorExtension;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class AuthenticationLogicImpl extends AbstractLogicImpl<IAuthenticationLogic.View> implements IAuthenticationLogic {
	
	private IAuthenticatorExtension authenticator;
	
	public AuthenticationLogicImpl(Context context, AbstractLogicImpl<?> parent) {
		super(context, parent);
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.authenticator");

		// Authenticator retrieval
		if (cfgs.length == 0) {
			authenticator = new DefaultAuthenticator(this);
		}
		else {
			if (cfgs.length > 1) {
				System.err.println(
						"More than one authenticator is provided.\n" +
						"Only one authenticator implementation is allowed");
			}
			IConfigurationElement cfg = cfgs[0];
			try {
				authenticator = (IAuthenticatorExtension) cfg.createExecutableExtension("class");
			}
			catch (CoreException e) {
				// If an error occurs, a null authenticator is instantiated
				// Nobody will be able to authenticate
				handleError(e);
				authenticator = new NullAuthenticator();
			}
		}
	}

	@Override
	public void onAuthenticate(String login, String password) {
		try {
			if (authenticator.authenticate(login, password)) {
				Collaborator collaborator = ModelMgr.getCollaborator(login);
				getContext().setConnectedCollaborator(collaborator);
				getEventBus().fire(new ConnectedCollaboratorEvent(this, collaborator));
			}
			else {
				getRoot().getView().showNotification("Invalid credentials.");
			}
		}
		catch (DbException e) {
			handleError(e);
		}
		
	}

}

class DefaultAuthenticator implements IAuthenticatorExtension {
	
	private AuthenticationLogicImpl parent;

	protected DefaultAuthenticator(AuthenticationLogicImpl parent) {
		this.parent = parent;
	}
	
	@Override
	public boolean authenticate(String login, String password) {
		try {
			return ModelMgr.getCollaborator(login) != null;
		}
		catch (DbException e) {
			parent.handleError(e);
			return false;
		}
	}

}

/**
 * Authenticator implementation that is used when a problem has occurred at startup.
 * 
 * <p>This implementation doesn't allow anybody to be authenticated.</p>
 * 
 * @author jbrazeau
 */
class NullAuthenticator implements IAuthenticatorExtension {
	
	@Override
	public boolean authenticate(String login, String password) {
		System.err.println("An error occured during startup, authentication refused to '" + login + "'");
		return false;
	}

}