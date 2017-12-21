package org.activitymgr.ui.web.logic.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.impl.internal.DbTransactionContext;
import org.activitymgr.ui.web.logic.impl.internal.ThreadLocalizedDbTransactionProviderImpl;
import org.activitymgr.ui.web.logic.spi.IRESTServiceLogic;

import com.google.inject.Inject;

public abstract class AbstractServiceLogic implements IRESTServiceLogic {

	@Inject
	private ThreadLocalizedDbTransactionProviderImpl dbTxProvider;

	@Override
	public void service(Parameters parameters, OutputStream response)
			throws IOException {
		boolean ctxReleased = true;
		try {
			DbTransactionContext ctx = dbTxProvider.newCtx();
			ctxReleased = false;
			Connection tx = ctx.getTx();

			doService(parameters, response);
			tx.commit();
			dbTxProvider.release();
			ctxReleased = true;
		} catch (ModelException e) {
			throw new IllegalStateException(e);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
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

	@Override
	public String getContentType() {
		return "application/octet-stream";
	}

	protected abstract void doService(Parameters parameters,
			OutputStream response) throws ModelException, IOException;

}
