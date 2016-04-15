package org.activitymgr.ui.web.logic.impl.internal;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.google.inject.Provider;

public class ThreadLocalizedDbTransactionProviderImpl implements Provider<DbTransactionContext> {
	
	private DataSource ds;

	private ThreadLocal<DbTransactionContext> transactions = new ThreadLocal<DbTransactionContext>();

	public ThreadLocalizedDbTransactionProviderImpl(DataSource ds) {
		this.ds = ds;
	}

	public DbTransactionContext newCtx() throws SQLException {
		DbTransactionContext dbTransactionContext = transactions.get();
		if (dbTransactionContext != null) {
			throw new IllegalStateException("Previous database context not released for the current thread");
		}
		dbTransactionContext = new DbTransactionContext(ds.getConnection());
		transactions.set(dbTransactionContext);
		return dbTransactionContext;
	}
	
	public void release() throws SQLException {
		transactions.get().getTx().close();
		transactions.remove();
	}

	@Override
	public DbTransactionContext get() {
		return transactions.get();
	}

}