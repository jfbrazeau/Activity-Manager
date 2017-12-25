package org.activitymgr.ui.web.logic.impl;

import java.io.IOException;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
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
		// First attempt : authentication cookie
		String login = request
				.getCookie(AuthenticationLogicImpl.NAME_COOKIE);
		if (login != null) {
			log.info("User authenticated through rememberme cookie : " + login);
		} else {
			// 2nd attempt : basic authentication
			// Retrieve HTTP "Authorization" header
			String authorizationHeader = request.getHeader("Authorization");
			if (authorizationHeader == null) {
				response.addHeader("WWW-Authenticate",
						"Basic realm=\"Check user access\"");
				throw new HttpException(401, "Not authorized");
			} else {
				// Retrieve "login/password" in Base64 string
				String couple = new String(
						Base64.decodeBase64(authorizationHeader
								.substring(6)));
				String[] split = couple.split(":");
				if (split.length == 2) {
					login = split[0];
					String password = split[1];
					if (authenticator.authenticate(login, password)) {
						log.info("User authenticated through basic authentication header : "
								+ login);
					}
				}
			}
		}
		// Retrieve collaborator
		Collaborator result = null;
		if (login != null) {
			result = modelMgr.getCollaborator(login);
		}
		if (result == null) {
			// If the user is not authenticated, send an HTTP error
			throw new HttpException(401, "Not authorized");
		} else {
			return result;
		}

	}

}
