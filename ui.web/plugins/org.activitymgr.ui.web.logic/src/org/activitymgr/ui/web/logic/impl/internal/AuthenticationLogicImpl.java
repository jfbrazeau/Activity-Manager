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

	private boolean afterLogout;

	public AuthenticationLogicImpl(ILogic<?> parent, boolean afterLogout) {
		super(parent);
		this.afterLogout = afterLogout;
	}

	@Override
	public void onViewAttached() {
		if (afterLogout) {
			getRoot().getView().setCookie(NAME_COOKIE, null);
		} else {
			// Init defaults
			String rememberedLogin = getRoot().getView().getCookie(NAME_COOKIE);
			if (rememberedLogin != null) {
				Collaborator collaborator = getModelMgr().getCollaborator(
						rememberedLogin);
				if (collaborator != null) {
					authenticationSuccessfull(collaborator);
				}
			}
		}
	}
	
	@Override
	public void onAuthenticate(String login, String password, boolean rememberMe) {
		// Cookie management
		if (rememberMe) {
			getRoot().getView().setCookie(AuthenticationLogicImpl.NAME_COOKIE, login);
		}
		else {
			getRoot().getView().removeCookie(AuthenticationLogicImpl.NAME_COOKIE);
		}
		// Authentication
		if (authenticator.authenticate(login, password)) {
			Collaborator collaborator = getModelMgr().getCollaborator(login);
			if (collaborator == null) {
				getRoot().getView().showNotification("User '" + login + "' has been authenticated but does not exist in the database. Please contact your administrator.");
			}
			else {
				authenticationSuccessfull(collaborator);
			}
		}
		else {
			getRoot().getView().showNotification("Invalid credentials.");
		}
	}

	private void authenticationSuccessfull(Collaborator collaborator) {
		((ILogicContext)getContext()).setConnectedCollaborator(collaborator);
		getEventBus().fire(new ConnectedCollaboratorEvent(this, collaborator));
	}

}

