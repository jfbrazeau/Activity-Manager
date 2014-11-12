package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.core.DAOException;
import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.IAuthenticatorExtension;
import org.activitymgr.ui.web.logic.impl.LogicContext;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class AuthenticationLogicImpl extends AbstractLogicImpl<IAuthenticationLogic.View> implements IAuthenticationLogic {
	
	private static final String NAME_COOKIE = "name";

	private IAuthenticatorExtension authenticator;
	
	public AuthenticationLogicImpl(ILogic<?> parent, LogicContext context) {
		super(parent, context);
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.authenticator");

		// Authenticator retrieval
		if (cfgs.length == 0) {
			authenticator = new DefaultAuthenticator(this, getContext().getComponent(IModelMgr.class));
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
	protected Object[] getViewParameters() {
		return new String[] { getRoot().getView().getCookie(NAME_COOKIE) };
	}

	@Override
	public void onAuthenticate(String login, String password, boolean rememberMe) {
		try {
			// Cookie management
			if (rememberMe) {
				getRoot().getView().setCookie(NAME_COOKIE, login);
			}
			else {
				getRoot().getView().removeCookie(NAME_COOKIE);
			}
			// Authentication
			if (authenticator.authenticate(login, password)) {
				Collaborator collaborator = getModelMgr().getCollaborator(login);
				((LogicContext)getContext()).setConnectedCollaborator(collaborator);
				getEventBus().fire(new ConnectedCollaboratorEvent(this, collaborator));
			}
			else {
				getRoot().getView().showNotification("Invalid credentials.");
			}
		}
		catch (DAOException e) {
			handleError(e);
		}
		
	}

	@Override
	protected void handleError(Throwable error) {
		super.handleError(error);
	}

}

class DefaultAuthenticator implements IAuthenticatorExtension {
	
	private AuthenticationLogicImpl parent;
	private IModelMgr modelMgr;

	protected DefaultAuthenticator(AuthenticationLogicImpl parent, IModelMgr modelMgr) {
		this.parent = parent;
		this.modelMgr = modelMgr;
	}
	
	@Override
	public boolean authenticate(String login, String password) {
		try {
			return modelMgr.getCollaborator(login) != null;
		}
		catch (DAOException e) {
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