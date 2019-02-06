package org.activitymgr.ui.web.logic.impl;

import java.io.IOException;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.impl.internal.AuthenticationLogicImpl;
import org.activitymgr.ui.web.logic.spi.IAuthenticatorExtension;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public abstract class AbstractServiceWithAuthenticationLogic extends
		AbstractServiceWithDbAccessLogic {

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractServiceWithAuthenticationLogic.class);

	@Inject
	private IAuthenticatorExtension authenticator;

	@Inject
	private IModelMgr modelMgr;

	@Override
	protected final void doService(Request request, Response response)
			throws ModelException, IOException, HttpException {
			// Check authentication
		Collaborator connected = checkAuthentication(request, response);
		doService(connected, request, response);
	}

	protected abstract void doService(Collaborator connected, Request request,
			Response response) throws ModelException, IOException;

	private Collaborator checkAuthentication(Request request, Response response)
			throws HttpException {
		// Retrieve all potential authentication data
		String authorizationHeader = request.getHeader("Authorization");
		IUILogicContext uiContext = request.getAttachedUILogicContext();
		String nameCookie = request.getCookie(AuthenticationLogicImpl.NAME_COOKIE);
		// 1st attempt : authorization header
		if (authorizationHeader != null) {
			// Retrieve "login/password" in Base64 string
			String couple = new String(Base64.decodeBase64(authorizationHeader
					.substring(6)));
			String[] split = couple.split(":");
			if (split.length == 2) {
				String login = split[0];
				String password = split[1];
				if (authenticator.authenticate(login, password)) {
					log.info("User authenticated through basic authentication header : "
							+ login);
					return modelMgr.getCollaborator(login);
				}
			}
		}
		// 2nd attempt : try to find the UI context (exists if the URL
		// contains a v-uiId parameter
		else if (uiContext != null
					&& uiContext.getConnectedCollaborator() != null) {
			return uiContext.getConnectedCollaborator();

		}
		// Third attempt : is the name cookie present ?
		else if (nameCookie != null) {
			return modelMgr.getCollaborator(nameCookie);
		}
		// At this stage, if the user has not been authenticated, reject the request
		response.addHeader("WWW-Authenticate",
				"Basic realm=\"Check user access\"");
		throw new HttpException(401, "Not authorized");
	}


}
