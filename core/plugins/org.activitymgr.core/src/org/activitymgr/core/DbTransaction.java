/*
 * Copyright (c) 2004-2012, Jean-Francois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.activitymgr.core.util.DbHelper;
import org.apache.log4j.Logger;

/**
 * Contexte de transaction.
 */
public class DbTransaction {

	/** Logger */
	private static Logger log = Logger.getLogger(DbTransaction.class);

	/** Connexion SQL */
	private Connection con;

	/**
	 * Constructeur par défaut.
	 * 
	 * @param con
	 *            connexion à la base de données.
	 */
	public DbTransaction(Connection con) {
		this.con = con;
	}

	/**
	 * @return la connexion à la base de données.
	 */
	public Connection getConnection() {
		return con;
	}

	/**
	 * Creates a statement for the current transaction.
	 * @return the newly created statement.
	 * @throws SQLException thrown  if a database access error occurs.
	 */
	public Statement createStatement() throws SQLException {
		return con.createStatement();
	}
	
	/**
	 * Prépare une requête SQL.
	 * 
	 * @param sql
	 *            requête SQL.
	 * @param generatedKey 
	 * 	<code>true</code> if the generated key must be returned.
	 * @return la requête initialisée.
	 * @throws SQLException
	 *             en cas d'erreur lié à la BDD.
	 */
	public PreparedStatement prepareStatement(String sql, boolean generatedKey)
			throws SQLException {
		log.debug(sql);
		return generatedKey ? con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : con.prepareStatement(sql);
	}

	/**
	 * Prépare une requête SQL.
	 * 
	 * @param sql
	 *            requête SQL.
	 * @return la requête initialisée.
	 * @throws SQLException
	 *             en cas d'erreur lié à la BDD.
	 */
	public PreparedStatement prepareStatement(String sql)
			throws SQLException {
		log.debug(sql);
		return prepareStatement(sql, false);
	}

	/**
	 * Indique si la BDD de données est une base HSQLDB ou H2.
	 * 
	 * @return un booléen indiquant si la BDD est de type HSQLDB ou H2.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public boolean isHsqlOrH2() throws DbException {
		return DbHelper.isHsqlOrH2(con);
	}

}
