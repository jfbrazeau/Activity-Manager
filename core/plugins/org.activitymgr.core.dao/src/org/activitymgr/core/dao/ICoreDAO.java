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
 * ON ANY THEORY OF LIABILITY, WHETHER IN ContributionRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.core.dao;

import java.io.InputStream;

/**
 * Composant offrant les services de base de persistence de l'application.
 */
public interface ICoreDAO {

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean tablesExist() throws DAOException;

	/**
	 * Vérifie si une table existe dans le modèle.
	 * 
	 * @param tableName
	 *            le nom de la table.
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean tableExists(String tableName) throws DAOException;

	/**
	 * Crée les tables du modèle de données.
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void createTables() throws DAOException;

	/**
	 * Executes a SQL script.
	 * 
	 * @param scriptContent
	 *            the script content.
	 * @throws DAOException
	 *             thrown if a database error occurs.
	 */
	void executeScript(InputStream scriptContent) throws DAOException;

	/**
	 * Executes a SQL script.
	 * 
	 * @param scriptContent
	 *            the script content.
	 * @throws DAOException
	 *             thrown if a database error occurs.
	 */
	void executeScript(String scriptContent) throws DAOException;

	/**
	 * Indique si la BDD de données est une base HSQLDB ou H2.
	 * 
	 * @return un booléen indiquant si la BDD est de type HSQLDB ou H2.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean isHsqlOrH2() throws DAOException;

}
