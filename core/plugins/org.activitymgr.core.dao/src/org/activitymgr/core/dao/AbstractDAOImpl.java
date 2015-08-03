package org.activitymgr.core.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AbstractDAOImpl {
	
	/** Transaction provider */
	@Inject
	private Provider<Connection> tx;
	
	/**
	 * @return the active connection.
	 */
	protected Connection tx() {
		return tx.get();
	}

	/**
	 * Tries to close in a last attempt the {@link Statement}.
	 * 
	 * @param stmt
	 *            the {@link Statement} to close.
	 */
	protected void lastAttemptToClose(Statement stmt) {
		if (stmt != null)
			try {
				stmt.close();
			} catch (Throwable ignored) {
			}
	}

	/**
	 * Tries to close in a last attempt the {@link ResultSet}.
	 * 
	 * @param rs
	 *            the {@link ResultSet} to close.
	 */
	protected void lastAttemptClose(ResultSet rs) {
		if (rs != null)
			try {
				rs.close();
			} catch (Throwable ignored) {
			}
	}


}
