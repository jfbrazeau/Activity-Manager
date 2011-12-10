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
package jfb.tools.activitymgr.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
	 * Constructeur par d�faut.
	 * 
	 * @param con
	 *            connexion � la base de donn�es.
	 */
	protected DbTransaction(Connection con) {
		this.con = con;
	}

	/**
	 * @return la connexion � la base de donn�es.
	 */
	public Connection getConnection() {
		return con;
	}

	/**
	 * Pr�pare une requ�te SQL.
	 * 
	 * @param sql
	 *            requ�te SQL.
	 * @return la requ�te initialis�e.
	 * @throws SQLException
	 *             en cas d'erreur li� � la BDD.
	 */
	protected PreparedStatement prepareStatement(String sql)
			throws SQLException {
		log.debug(sql);
		return con.prepareStatement(sql);
	}

}
