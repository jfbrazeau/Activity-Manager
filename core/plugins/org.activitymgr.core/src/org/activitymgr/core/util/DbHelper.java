package org.activitymgr.core.util;

import java.sql.Connection;
import java.sql.SQLException;

import org.activitymgr.core.DbException;

public class DbHelper {

	/**
	 * Indique si la BDD de données est une base HSQLDB ou H2 embarquée.
	 * 
	 * @param con
	 *            la connexion SQL.
	 * @param jdbcUrl
	 *            hte database JDBC url.
	 * @return un booléen indiquant si la BDD est de type HSQLDB ou H2
	 *         embarquée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static boolean isEmbeddedHsqlOrH2(Connection con, String jdbcUrl)
			throws DbException {
		// Récupération du nom de la base de données
		return isHsqlOrH2(con)
				&& (jdbcUrl.startsWith("jdbc:hsqldb:file") || jdbcUrl.startsWith("jdbc:h2:file")); //$NON-NLS-1$
	}

	/**
	 * Indique si la BDD de données est une base HSQLDB ou H2.
	 * 
	 * @param con
	 *            la connexion SQL.
	 * @return un booléen indiquant si la BDD est de type HSQLDB ou H2.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static boolean isHsqlOrH2(Connection con) throws DbException {
		try {
			// Récupération du nom de la base de données
			String dbName = con.getMetaData().getDatabaseProductName()
					.toLowerCase();
			return dbName.contains("hsql") || dbName.contains("h2"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (SQLException e) {
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_DATABASE_NAME_EXTRACTION_FAILURE"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Shutdowns the database.
	 * 
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static void shutdowHsqlOrH2(Connection con) throws DbException {
		try {
			con.createStatement().execute("shutdown"); //$NON-NLS-1$
		} catch (SQLException e) {
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_DISCONNECTION_FAILURE"), e); //$NON-NLS-1$
		}
	}

}
