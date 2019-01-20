package org.activitymgr.ui.web.logic.impl.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.IConfiguration;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.spi.IAuthenticatorExtension;
import org.apache.poi.util.IOUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;

public class AuthenticationLogicImpl extends
		AbstractLogicImpl<IAuthenticationLogic.View> implements
		IAuthenticationLogic {

	public static final String NAME_COOKIE = "name";

	@Inject
	private IAuthenticatorExtension authenticator;

	@Inject
	private IDTOFactory factory;

	@Inject
	private IConfiguration configuration;

	private boolean afterLogout;

	public AuthenticationLogicImpl(ILogic<?> parent, boolean afterLogout) {
		super(parent);
		this.afterLogout = afterLogout;
		String googleSignInClientId = configuration
				.get("google-signin.client_id");
		if (googleSignInClientId != null) {
			getView().setGoogleSignInClientId(googleSignInClientId);
		}
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
			getRoot().getView().setCookie(AuthenticationLogicImpl.NAME_COOKIE,
					login);
		} else {
			getRoot().getView().removeCookie(
					AuthenticationLogicImpl.NAME_COOKIE);
		}
		// Authentication
		if (authenticator.authenticate(login, password)) {
			Collaborator collaborator = getModelMgr().getCollaborator(login);
			if (collaborator == null) {
				getRoot()
						.getView()
						.showNotification(
								"User '"
										+ login
										+ "' has been authenticated but does not exist in the database. Please contact your administrator.");
			} else {
				authenticationSuccessfull(collaborator);
			}
		} else {
			getRoot().getView().showNotification("Invalid credentials.");
		}
	}

	@Override
	public void onAuthenticateWithGoogle(String idToken) {
		try {
			URL url = new URL(
					"https://oauth2.googleapis.com/tokeninfo?id_token="
							+ idToken);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = url.openStream();
			IOUtils.copy(in, out);
			in.close();
			JsonObject json = (JsonObject) new JsonParser().parse(new String(
					out.toByteArray()));
			String email = getAttribute(json, "email");
			Collaborator collaborator = getModelMgr().getCollaborator(email);
			if (collaborator == null) {
				// Auto create the account if a default domain has been set
				String autoCreateDomain = configuration
						.get("google-signin.auto_create_account_for_domain");
				if (autoCreateDomain != null && email.toLowerCase().endsWith('@' + autoCreateDomain.toLowerCase())) {
					collaborator = factory.newCollaborator();
					collaborator.setLogin(email);
					String lastName = getAttribute(json, "family_name");
					collaborator.setLastName(lastName);
					String firstName = getAttribute(json, "given_name");
					collaborator.setFirstName(firstName);
					collaborator = getModelMgr().createCollaborator(collaborator);
				}
			}
			if (collaborator == null) {
				getRoot()
						.getView()
						.showNotification(
								"User '"
										+ email
										+ "' is not allowed to sign-in. Please contact your administrator.");
			} else {
				authenticationSuccessfull(collaborator);
			}

		} catch (IOException e) {
			getRoot().getView().showErrorNotification("Google sign in failed",
					e.getMessage());
		} catch (ModelException e) {
			getRoot().getView().showErrorNotification(
					"Unexpected error while signing in", e.getMessage());
		}

	}

	private String getAttribute(JsonObject json, String id) throws IOException {
		JsonElement attribute = json.get(id);
		if (attribute == null) {
			throw new IOException("Missing attribute " + id);
		}
		return attribute.getAsString();
	}

	private void authenticationSuccessfull(Collaborator collaborator) {
		((ILogicContext) getContext()).setConnectedCollaborator(collaborator);
		getEventBus().fire(new ConnectedCollaboratorEvent(this, collaborator));
	}

}
