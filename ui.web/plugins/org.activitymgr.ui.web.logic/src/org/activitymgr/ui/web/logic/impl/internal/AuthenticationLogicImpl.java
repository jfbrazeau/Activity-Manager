package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.spi.IAuthenticatorExtension;

import com.google.inject.Inject;

public class AuthenticationLogicImpl extends AbstractLogicImpl<IAuthenticationLogic.View> implements IAuthenticationLogic {
	
	private static final String NAME_COOKIE = "name";

	@Inject
	private IAuthenticatorExtension authenticator;
	
	public AuthenticationLogicImpl(ILogic<?> parent) {
		super(parent);
		// Init defaults
		String defaultLogin = getRoot().getView().getCookie(NAME_COOKIE);
		getView().setDefaults(defaultLogin, defaultLogin != null);
	}

	@Override
	public void onAuthenticate(String login, String password, boolean rememberMe) {
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
			((ILogicContext)getContext()).setConnectedCollaborator(collaborator);
			getEventBus().fire(new ConnectedCollaboratorEvent(this, collaborator));
		}
		else {
			getRoot().getView().showNotification("Invalid credentials.");
		}
	}

}

