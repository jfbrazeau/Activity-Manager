package org.activitymgr.ui.web.logic.impl;

import java.io.IOException;
import java.sql.Connection;

import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.impl.internal.DbTransactionContext;
import org.activitymgr.ui.web.logic.impl.internal.ThreadLocalizedDbTransactionProviderImpl;
import org.activitymgr.ui.web.logic.spi.IRESTServiceLogic;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public abstract class AbstractServiceWithDbAccessLogic implements IRESTServiceLogic {

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
	private static Logger log = Logger.getLogger(AbstractServiceWithDbAccessLogic.class);

	@Inject
	private ThreadLocalizedDbTransactionProviderImpl dbTxProvider;

	@Override
	public final void service(Request request, Response response)
			throws IOException {
		boolean ctxReleased = true;
		try {
			// Init tx
			DbTransactionContext ctx = dbTxProvider.newCtx();
			ctxReleased = false;
			Connection tx = ctx.getTx();

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

	protected abstract void doService(Request request, Response response)
			throws ModelException, IOException, HttpException;

}
