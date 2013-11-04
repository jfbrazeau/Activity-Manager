package org.activitymgr.core;

import java.sql.Connection;
import java.sql.SQLException;

import org.activitymgr.core.util.Strings;
import org.apache.commons.dbcp.BasicDataSource;

import com.google.inject.Provider;

/**
 * Thread local transaction provider.
 * 
 * <p>
 * This provider attaches transactions to the current {@link Thread}.
 * </p>
 * 
 * @author jbrazeau
 */
public class ThreadLocalTxProvider implements Provider<DbTransaction> {

	/** Datasource */
	private BasicDataSource datasource;

	/** Thread local connections */
	private ThreadLocal<DbTransaction> tx = new ThreadLocal<DbTransaction>();

	/**
	 * Default constructor.
	 * 
	 * @param tx
	 *            transaction provider.
	 * @param driverName
	 *            the JDBC driver name.
	 * @param url
	 *            the JDBC URL.
	 * @param user
	 *            the JDBC user.
	 * @param password
	 *            the JDBC password.
	 * @throws DbException
	 *             thrown if an error occurs during database connection.
	 */
	public ThreadLocalTxProvider(String driverName, String url, String user,
			String password) throws DbException {
		try {
			// Initialisation de la Datasource
			datasource = new BasicDataSource();
			datasource.setDriverClassName(driverName);
			datasource.setUrl(url);
			datasource.setUsername(user);
			datasource.setPassword(password);
			datasource.setDefaultAutoCommit(false);

			// Tentative de récupération d'une connexion
			// pour détecter les problèmes de connexion
			Connection con = datasource.getConnection();
			con.close();
		} catch (SQLException e) {
			throw new DbException(Strings.getString(
					"DbMgr.errors.SQL_CONNECTION_OPEN", e.getMessage()), e); //$NON-NLS-1$
		}
	}

	@Override
	public DbTransaction get() {
		try {
			DbTransaction tx = this.tx.get();
			if (tx == null) {
				tx = new DbTransaction(datasource.getConnection());
				this.tx.set(tx);
			}
			return tx;
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

}
