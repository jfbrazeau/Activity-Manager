package org.activitymgr.ui.web.logic.impl;

import java.io.IOException;
import java.sql.Connection;

import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.impl.internal.AuthenticationLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.DbTransactionContext;
import org.activitymgr.ui.web.logic.impl.internal.ThreadLocalizedDbTransactionProviderImpl;
import org.activitymgr.ui.web.logic.spi.IAuthenticatorExtension;
import org.activitymgr.ui.web.logic.spi.IRESTServiceLogic;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public abstract class AbstractServiceLogic implements IRESTServiceLogic {

	@SuppressWarnings("serial")
	public static class HttpException extends Exception {
		private int code;

		public HttpException(int code, String message) {
			super(message);
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractServiceLogic.class);

	@Inject
	private ThreadLocalizedDbTransactionProviderImpl dbTxProvider;

	@Inject
	private IAuthenticatorExtension authenticator;

	@Override
	public void service(Request request, Response response)
			throws IOException {
		boolean ctxReleased = true;
		try {
			// Init tx
			DbTransactionContext ctx = dbTxProvider.newCtx();
			ctxReleased = false;
			Connection tx = ctx.getTx();

			// Check authentication
			checkAuthentication(request, response);

			// Execute service
			doService(request, response);

			// Default content type
			if (response.getContentType() == null) {
				response.setContentType("application/octet-stream");
			}
			
			// Commit & release tx
			tx.commit();
			dbTxProvider.release();
			ctxReleased = true;
		} catch (Throwable t) {
			if (t instanceof HttpException) {
				HttpException exception = (HttpException) t;
				log.warn(t.getMessage(), t);
				response.sendError(exception.getCode(), exception.getMessage());
			} else {
				log.error(t.getMessage(), t);
				response.sendError(500, t.getMessage());
			}
		} finally {
			if (!ctxReleased) {
				// Release the transaction (last attempt to close)
				try {
					dbTxProvider.release();
				} catch (Throwable ignored) {
				}
			}
		}
	}

	private void checkAuthentication(Request request, Response response) throws HttpException {
		// First attempt : authentication cookie
		String login = request
				.getCookie(AuthenticationLogicImpl.NAME_COOKIE);
		if (login != null) {
			log.info("User authenticated through rememberme cookie : " + login);
			return;
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
						return;
					}
				}
			}
		}
		// If the user is not authenticated, send an HTTP error
		throw new HttpException(401, "Not authorized");

	}

	protected abstract void doService(Request request, Response response)
			throws ModelException, IOException;

}
