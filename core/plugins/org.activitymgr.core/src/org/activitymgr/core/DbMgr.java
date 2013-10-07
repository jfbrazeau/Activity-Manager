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

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

/**
 * Classe offrant les services de base de persistence de l'application.
 */
public class DbMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(DbMgr.class);

	/** Formatteur de date */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

	/** Datasource */
	private BasicDataSource ds = null;

	/**
	 * Contexte de thread utilisé pour détecter les anomalies associées à la
	 * gestion de transaction
	 */
	private ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();

	/**
	 * Default constructor.
	 * 
	 * @param driverName
	 *            le nom du driver JDBC.
	 * @param url
	 *            l'URL de connexion au serveur.
	 * @param user
	 *            l'identifiant de connexion/
	 * @param password
	 *            le mot de passe de connexion.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public DbMgr(String driverName, String url,
			String user, String password) throws DbException {
		try {
			// Si la datasource existe on la ferme
			if (ds != null) {
				closeDatabaseAccess();
			}
			// Fermeture de la datasource
			BasicDataSource newDs = new BasicDataSource();

			// Initialisation de la Datasource
			newDs = new BasicDataSource();
			log.info("Connecting to '" + url + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			newDs.setDriverClassName(driverName);
			newDs.setUrl(url);
			newDs.setUsername(user);
			newDs.setPassword(password);
			newDs.setDefaultAutoCommit(false);

			// Tentative de récupération d'une connexion
			// pour détecter les problèmes de connexion
			Connection con = newDs.getConnection();
			con.close();

			// Sauvegarde de la référence
			ds = newDs;
		} catch (SQLException e) {
			log.info("SQL Exception", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.SQL_CONNECTION_OPEN", e.getMessage()), e); //$NON-NLS-1$
		}
	}

	/**
	 * Ferme la base de données.
	 * 
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la BDD.
	 */
	protected void closeDatabaseAccess() throws DbException {
		try {
			if (ds != null) {
				// Récupération de la connexion
				Connection con = ds.getConnection();

				// Cas d'une base HSQLDB embarquée
				if (isEmbeddedHSQLDB(con)) {
					// Extinction de la base de données
					con.createStatement().execute("shutdown"); //$NON-NLS-1$
				}

				// Fermeture de la datasource
				ds.close();
				ds = null;
			}
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_DISCONNECTION_FAILURE"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Permet de commencer une transaction.
	 * 
	 * <p>
	 * Une connexion à la base de données est établie. Celle ci doit être
	 * validée par la couche appelante par une invocation de
	 * <code>endTransaction</code>.
	 * </p>
	 * 
	 * @return le contexte de transaction.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected DbTransaction beginTransaction() throws DbException {
		try {
			// Est-on connecté à la BDD ?
			if (ds == null)
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_CONNECTION_ESTABLISHMENT_FAILURE"), null); //$NON-NLS-1$
			// Obtention d'une connexion
			if (threadLocal.get() != null)
				throw new Error(
						Strings.getString("DbMgr.errors.SQL_MULTI_TRANSACTION_DETECTED")); //$NON-NLS-1$
			Connection con = ds.getConnection();
			threadLocal.set(con);
			// log.debug("Active : " + ds.getNumActive() + ", Idle : " +
			// ds.getNumIdle() + ", Connexion : " + con);
			return new DbTransaction(con);
		} catch (SQLException e) {
			log.info("SQL Exception", e); //$NON-NLS-1$
			throw new DbException("Couldn't get a SQL Connection", e); //$NON-NLS-1$
		}
	}

	/**
	 * Valide une transactrion.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void commitTransaction(DbTransaction tx)
			throws DbException {
		try {
			tx.getConnection().commit();
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_COMMIT_FAILURE"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected boolean tablesExist(DbTransaction tx) throws DbException {
		boolean tablesExist = true;
		tablesExist &= tableExists(tx, "COLLABORATOR"); //$NON-NLS-1$
		tablesExist &= tableExists(tx, "CONTRIBUTION"); //$NON-NLS-1$
		tablesExist &= tableExists(tx, "DURATION"); //$NON-NLS-1$
		tablesExist &= tableExists(tx, "TASK"); //$NON-NLS-1$
		return tablesExist;
	}

	/**
	 * Vérifie si une table existe dans le modèle.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param tableName
	 *            le nom de la table.
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected boolean tableExists(DbTransaction tx, String tableName)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Recherche de la table
			ResultSet rs = con.getMetaData().getTables(null, null, tableName,
					new String[] { "TABLE" }); //$NON-NLS-1$

			// Récupération du résultat
			boolean exists = rs.next();

			// Retour du résultat
			return exists;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.SQL_TABLES_DETECTION_FAILURE", tableName), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Crée les tables du modèle de données.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void createTables(DbTransaction tx) throws DbException {
		// Récupération de la connexion
		Connection con = tx.getConnection();

		// Lecture du fichier SQL de création de la BDD
		String batchName = "sql/" + (isHSQLDB(con) ? "hsqldb.sql" : "mysqldb.sql"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		InputStream in = DbMgr.class.getResourceAsStream(batchName);
		executeScript(tx, in);

		// Test de l'existence des tables
		if (!tablesExist(tx))
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_TABLE_CREATION_FAILURE"), null); //$NON-NLS-1$

	}

	/**
	 * Executes a SQL script.
	 * 
	 * @param tx
	 *            the transaction context.
	 * @param scriptContent
	 *            the script content.
	 * @throws DbException
	 *             thrown if a database error occurs.
	 */
	protected void executeScript(DbTransaction tx, InputStream scriptContent)
			throws DbException {
		try {
			// Script content retrieval
			String sql = StringHelper.fromInputStream(scriptContent);
	
			// Execute the script
			executeScript(tx, sql);
		} catch (IOException e) {
			log.info("I/O error while loading table creation SQL script.", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_SCRIPT_LOAD_FAILURE"), null); //$NON-NLS-1$
		}
	}

	/**
	 * Executes a SQL script.
	 * 
	 * @param tx
	 *            the transaction context.
	 * @param scriptContent
	 *            the script content.
	 * @throws DbException
	 *             thrown if a database error occurs.
	 */
	protected void executeScript(DbTransaction tx, String scriptContent)
			throws DbException {
		Statement stmt = null;
		try {
			// Découpage et exécution du batch
			stmt = tx.getConnection().createStatement();
			LineNumberReader lnr = new LineNumberReader(new StringReader(
					scriptContent));
			StringBuffer buf = new StringBuffer();
			boolean proceed = true;
			do {
				String line = null;
				// On ne lit dans le flux que si la ligne courante n'est pas
				// encore totalement traitée
				if (line == null) {
					try {
						line = lnr.readLine();
					} catch (IOException e) {
						log.info("Unexpected I/O error while reading memory stream!", e); //$NON-NLS-1$
						throw new DbException(
								Strings.getString("DbMgr.errors.MEMORY_IO_FAILURE"), null); //$NON-NLS-1$
					}
					log.debug("Line read : '" + line + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				// Si le flux est vide, on sort de la boucle
				if (line == null) {
					proceed = false;
				}
				// Sinon on traite la ligne
				else {
					line = line.trim();
					// Si la ligne est un commentaire on l'ignore
					if (line.startsWith("--")) { //$NON-NLS-1$
						line = null;
					} else {
						// Sinon on regarde si la ligne possède
						// un point virgule
						int idx = line.indexOf(';');
						// Si c'est le cas, on découpe la chaîne et on
						// exécute la requête
						if (idx >= 0) {
							buf.append(line.subSequence(0, idx));
							line = line.substring(idx);
							String sql = buf.toString();
							buf.setLength(0);
							log.debug(" - sql='" + sql + "'"); //$NON-NLS-1$ //$NON-NLS-2$
							if (!"".equals(sql)) //$NON-NLS-1$
								stmt.executeUpdate(sql);
						}
						// sinon on ajoute la ligne au buffer de requête
						else {
							buf.append(line);
							buf.append('\n');
						}
					}
				}

			} while (proceed);

			// Fermeture du statement
			stmt.close();
			stmt = null;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException("Database table creation failure", e); //$NON-NLS-1$
		} 
		finally {
				if (stmt != null)
					try {
						stmt.close();
					} catch (Throwable ignored) {
					}
		}
	}

	/**
	 * Crée un collaborateur.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param newCollaborator
	 *            le collaborateur à créer.
	 * @return le collaborateur après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Collaborator createCollaborator(DbTransaction tx,
			Collaborator newCollaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("insert into COLLABORATOR (clb_login, clb_first_name, clb_last_name, clb_is_active) values (?, ?, ?, ?)", true); //$NON-NLS-1$
			pStmt.setString(1, newCollaborator.getLogin());
			pStmt.setString(2, newCollaborator.getFirstName());
			pStmt.setString(3, newCollaborator.getLastName());
			pStmt.setBoolean(4, newCollaborator.getIsActive());
			pStmt.executeUpdate();

			// Récupération de l'identifiant généré
			long generatedId = getGeneratedId(pStmt);
			log.debug("Generated id=" + generatedId); //$NON-NLS-1$
			newCollaborator.setId(generatedId);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return newCollaborator;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_CREATION_FAILUE", newCollaborator.getLogin()), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Crée une contribution.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param newContribution
	 *            la nouvelle contribution.
	 * @return la contribution après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Contribution createContribution(DbTransaction tx,
			Contribution newContribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("insert into CONTRIBUTION (ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration) values (?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$
			pStmt.setInt(1, newContribution.getYear());
			pStmt.setInt(2, newContribution.getMonth());
			pStmt.setInt(3, newContribution.getDay());
			pStmt.setLong(4, newContribution.getContributorId());
			pStmt.setLong(5, newContribution.getTaskId());
			pStmt.setLong(6, newContribution.getDurationId());
			pStmt.executeUpdate();

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return newContribution;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTION_CREATION_FAILUE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Crée une contribution.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param newDuration
	 *            la nouvelle durée.
	 * @return la durée après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Duration createDuration(DbTransaction tx,
			Duration newDuration) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("insert into DURATION (dur_id, dur_is_active) values (?, ?)"); //$NON-NLS-1$
			pStmt.setLong(1, newDuration.getId());
			pStmt.setBoolean(2, newDuration.getIsActive());
			pStmt.executeUpdate();

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return newDuration;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.DURATION_CREATION_FAILUE", newDuration), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Crée une tache.
	 * 
	 * <p>
	 * La tache parent peut être nulle pour indiquer que la nouvelle tache est
	 * une tache racine.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param parentTask
	 *            la tache parent accueillant la nouvelle tache.
	 * @param newTask
	 *            la nouvelle tache.
	 * @return la tache après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task createTask(DbTransaction tx, Task parentTask,
			Task newTask) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Mise à jour du chemin de la tâche
			String parentPath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
			newTask.setPath(parentPath);

			// Génération du numéro de la tâche
			byte taskNumber = newTaskNumber(tx, parentPath);
			newTask.setNumber(taskNumber);

			// Préparation de la requête
			pStmt = tx
					.prepareStatement("insert into TASK (tsk_path, tsk_number, tsk_code, tsk_name, tsk_budget, tsk_initial_cons, tsk_todo, tsk_comment) values (?, ?, ?, ?, ?, ?, ?, ?)", true); //$NON-NLS-1$
			pStmt.setString(1, newTask.getPath());
			pStmt.setString(2, StringHelper.toHex(newTask.getNumber()));
			pStmt.setString(3, newTask.getCode());
			pStmt.setString(4, newTask.getName());
			pStmt.setLong(5, newTask.getBudget());
			pStmt.setLong(6, newTask.getInitiallyConsumed());
			pStmt.setLong(7, newTask.getTodo());
			pStmt.setString(8, newTask.getComment());
			pStmt.executeUpdate();

			// Récupération de l'identifiant généré
			long generatedId = getGeneratedId(pStmt);
			log.debug("Generated id=" + generatedId); //$NON-NLS-1$
			newTask.setId(generatedId);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return newTask;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_CREATION_FAILURE", newTask.getName()), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Vérifie si la durée est utilisée en base.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la durée à vérifier.
	 * @return un booléen indiquant si la durée est utilisée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected boolean durationIsUsed(DbTransaction tx, Duration duration)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("select count(*) from CONTRIBUTION where ctb_duration=?"); //$NON-NLS-1$
			pStmt.setLong(1, duration.getId());

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			boolean durationIsUsed = rs.getInt(1) > 0;

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return durationIsUsed;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.SQL_DURATION_CHECK_FAILURE", duration), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Ferme une transactrion.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void endTransaction(DbTransaction tx) throws DbException {
		try {
			tx.getConnection().close();
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_DISCONNECTION_FAILURE"), e); //$NON-NLS-1$
		}
		threadLocal.set(null);
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param collaboratorId
	 *            l'identifiant du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Collaborator getCollaborator(DbTransaction tx,
			long collaboratorId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = tx
					.prepareStatement("select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from COLLABORATOR where clb_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, collaboratorId);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			Collaborator collaborator = null;
			if (rs.next())
				collaborator = rsToCollaborator(rs);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return collaborator;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_SELECTION_BY_ID_FAILURE", new Long(collaboratorId)), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Convertit le résultat d'une requête en collaborateur.
	 * 
	 * @param rs
	 *            le result set.
	 * @return le collaborateur.
	 * @throws SQLException
	 *             levé en cas de problème SQL.
	 */
	private Collaborator rsToCollaborator(ResultSet rs)
			throws SQLException {
		Collaborator collaborator = new Collaborator();
		collaborator.setId(rs.getLong(1));
		collaborator.setLogin(rs.getString(2));
		collaborator.setFirstName(rs.getString(3));
		collaborator.setLastName(rs.getString(4));
		collaborator.setIsActive(rs.getBoolean(5));
		return collaborator;
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param login
	 *            l'identifiant de connexion du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant de connexion est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Collaborator getCollaborator(DbTransaction tx, String login)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from COLLABORATOR where clb_login=?"); //$NON-NLS-1$
			pStmt.setString(1, login);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			Collaborator collaborator = null;
			if (rs.next())
				collaborator = rsToCollaborator(rs);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return collaborator;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_SELECTION_BY_LOGIN_FAILURE", login), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @param onlyActiveCollaborators
	 *            booléen indiquant si l'on ne doit retourner que les
	 *            collaborateurs actifs.
	 * @return la liste des collaborateurs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Collaborator[] getCollaborators(DbTransaction tx,
			int orderByClauseFieldIndex, boolean ascendantSort,
			boolean onlyActiveCollaborators) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer(
					"select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from COLLABORATOR "); //$NON-NLS-1$
			if (onlyActiveCollaborators)
				request.append("where clb_is_active=? "); //$NON-NLS-1$
			request.append("order by "); //$NON-NLS-1$
			switch (orderByClauseFieldIndex) {
			case Collaborator.ID_FIELD_IDX:
				request.append("clb_id"); //$NON-NLS-1$
				break;
			case Collaborator.LOGIN_FIELD_IDX:
				request.append("clb_login"); //$NON-NLS-1$
				break;
			case Collaborator.FIRST_NAME_FIELD_IDX:
				request.append("clb_first_name"); //$NON-NLS-1$
				break;
			case Collaborator.LAST_NAME_FIELD_IDX:
				request.append("clb_last_name"); //$NON-NLS-1$
				break;
			case Collaborator.IS_ACTIVE_FIELD_IDX:
				request.append("clb_is_active"); //$NON-NLS-1$
				break;
			default:
				throw new DbException(
						Strings.getString(
								"DbMgr.errors.UNKNOWN_FIELD_INDEX", new Integer(orderByClauseFieldIndex)), null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			request.append(ascendantSort ? " asc" : " desc"); //$NON-NLS-1$ //$NON-NLS-2$
			pStmt = tx.prepareStatement(request.toString());
			if (onlyActiveCollaborators)
				pStmt.setBoolean(1, true);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Collaborator> list = new ArrayList<Collaborator>();
			while (rs.next())
				list.add(rsToCollaborator(rs));

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			log.debug("  => found " + list.size() + " entrie(s)"); //$NON-NLS-1$ //$NON-NLS-2$
			return (Collaborator[]) list.toArray(new Collaborator[list.size()]);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.COLLABORATORS_SELECTION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param contributor
	 *            le collaborateur associé aux contributions.
	 * @param parentTask
	 *            la tache parente associée aux contributions (en général si parentTask != nul, task = null et vice versa).
	 * @param task
	 *            la tache associée aux contributions (en général si parentTask != nul, task = null et vice versa).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la liste des contributions associées aux paramétres spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Contribution[] getContributions(DbTransaction tx,
			Collaborator contributor, Task parentTask, Task task, Calendar fromDate,
			Calendar toDate) throws DbException {
		log.debug("getContributions(" + contributor + ", " + task + ", " + sdf.format(fromDate.getTime()) + ", " + sdf.format(toDate.getTime()) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Conversion des dates
			String fromDateStr = sdf.format(fromDate.getTime());
			String toDateStr = sdf.format(toDate.getTime());

			// Préparation de la requête
			StringWriter request = new StringWriter();
			request.append("select ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration");
			if (parentTask != null) {
				request.append(", tsk_id, tsk_path");
			}
			request.append(" from CONTRIBUTION");
			if (parentTask != null) {
				request.append(", TASK");
			}
			request.append(" where");
			if (parentTask != null) {
				request.append(" ctb_task=tsk_id and tsk_path like ? and");
			}
			request.append(" ctb_year*10000 + ( ctb_month*100 + ctb_day )");
			if (!fromDateStr.equals(toDateStr)) {
				request.append(" between ? and ?");
			} else {
				request.append(" = ?");
			}
			if (contributor != null) {
				request.append(" and ctb_contributor=?");
			}
			if (task != null) {
				request.append(" and ctb_task=?");
			}
			pStmt = tx.prepareStatement(request.toString());
			int paramIdx = 1;
			// Parent task management
			if (parentTask != null) {
				pStmt.setString(paramIdx++, parentTask.getFullPath() + '%');
			}
			// 1° cas : les deux dates sont différentes
			if (!fromDateStr.equals(toDateStr)) {
				pStmt.setString(paramIdx++, fromDateStr);
				pStmt.setString(paramIdx++, toDateStr);
			}
			// 2° cas : les deux dates sont égales
			else {
				pStmt.setString(paramIdx++, fromDateStr);
			}
			if (contributor != null) {
				pStmt.setLong(paramIdx++, contributor.getId());
			}
			if (task != null) {
				pStmt.setLong(paramIdx++, task.getId());
			}

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Extraction du résultat
			Contribution[] result = rsToContributions(rs);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTIONS_SELECTION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Extrait les contributions du resultat de la requête SQL.
	 * 
	 * @param rs
	 *            le résultat de la requête SQL.
	 * @return les contributions extraites.
	 * @throws SQLException
	 *             levé en cas d'incident avec la base de données.
	 */
	private Contribution[] rsToContributions(ResultSet rs)
			throws SQLException {
		// Recherche des sous-taches
		ArrayList<Contribution> list = new ArrayList<Contribution>();
		while (rs.next()) {
			// Préparation du résultat
			Contribution contribution = new Contribution();
			contribution.setYear(rs.getInt(1));
			contribution.setMonth(rs.getInt(2));
			contribution.setDay(rs.getInt(3));
			contribution.setContributorId(rs.getInt(4));
			contribution.setTaskId(rs.getInt(5));
			contribution.setDurationId(rs.getLong(6));
			list.add(contribution);
		}
		log.debug("  => found " + list.size() + " entrie(s)"); //$NON-NLS-1$ //$NON-NLS-2$
		return (Contribution[]) list.toArray(new Contribution[list.size()]);
	}

	/**
	 * Retourne les contributions associées aux paramétres spécifiés.
	 * 
	 * <p>
	 * Tous les paramétres sont facultatifs. Chaque paramétre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramétre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.
	 * </p>
	 * 
	 * <p>
	 * La spécification des paramétres répond aux mêmes règles que pour la
	 * méthode <code>getContributionsSum</code>.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return les contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgr#getContributionsSum(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	protected Contribution[] getContributions(DbTransaction tx,
			Task task, Collaborator contributor, Integer year, Integer month,
			Integer day) throws DbException {
		log.debug("getContributions(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			StringBuffer baseRequest = new StringBuffer(
					"select ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration from CONTRIBUTION, TASK where ctb_task=tsk_id"); //$NON-NLS-1$
			String orderByClause = " order by ctb_year, ctb_month, ctb_day, tsk_path, tsk_number, ctb_contributor, ctb_duration"; //$NON-NLS-1$
			// Cas ou la tache n'est pas spécifiée
			if (task == null) {
				// Préparation de la requête
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				baseRequest.append(orderByClause);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				completeContributionReqParams(pStmt, 1, contributor, year,
						month, day);
			}
			// Si la tache n'admet pas de sous-taches, le cumul de
			// budget, de consommé initial, de reste à faire sont
			// égaux à ceux de la tache
			else if (task.getSubTasksCount() == 0) {
				// Préparation de la requête
				baseRequest.append(" and tsk_id=?"); //$NON-NLS-1$
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				baseRequest.append(orderByClause);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				pStmt.setLong(1, task.getId());
				log.debug(" taskId=" + task.getId()); //$NON-NLS-1$
				completeContributionReqParams(pStmt, 2, contributor, year,
						month, day);
			}
			// Sinon, il faut calculer
			else {
				// Paramètre pour la clause 'LIKE'
				String pathLike = task.getFullPath() + "%"; //$NON-NLS-1$

				// Préparation de la requête
				baseRequest.append(" and tsk_path like ?"); //$NON-NLS-1$
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				baseRequest.append(orderByClause);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				pStmt.setString(1, pathLike);
				completeContributionReqParams(pStmt, 2, contributor, year,
						month, day);
			}

			// Exécution de la requête
			log.debug("Request : " + baseRequest); //$NON-NLS-1$
			rs = pStmt.executeQuery();

			// Extraction du résultat
			Contribution[] result = rsToContributions(rs);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTIONS_SELECTION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			try {
				if (pStmt != null)
					pStmt.close();
			} catch (Throwable ignored) {
			}
		}
	}

	/**
	 * Calcule le total des contributions associée aux paramétres spécifiés.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected long getContributionsSum(DbTransaction tx, Task task,
			Collaborator contributor, Calendar fromDate, Calendar toDate)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer(
					"select sum(ctb_duration) from CONTRIBUTION");
			if (task != null)
				request.append(", TASK");
			if (contributor != null)
				request.append(", COLLABORATOR");
			request.append(" where ");
			if (task != null) {
				request.append("ctb_task=tsk_id and tsk_id=?");
			}
			if (contributor != null) {
				if (task != null)
					request.append(" and ");
				request.append("ctb_contributor=clb_id and clb_id=?");
			}
			// Y'a-t-il un critère de date à ajouter ?
			if (fromDate != null || toDate != null) {
				request.append(" and ( ctb_year*10000 + ( ctb_month*100 + ctb_day ) )");
				// Deux dates spécifiées
				if (fromDate != null && toDate != null)
					request.append(" between ? and ?");
				// Seule la date de début spécifiée
				else if (fromDate != null)
					request.append(">=?");
				// Seule la date de fin spécifiée
				else
					request.append("<=?");
			}

			// Calcul du consommé
			log.debug("request : " + request);
			pStmt = tx.prepareStatement(request.toString()); //$NON-NLS-1$
			int idx = 1;
			if (task != null)
				pStmt.setLong(idx++, task.getId());
			if (contributor != null)
				pStmt.setLong(idx++, contributor.getId());

			// Y'a-t-il un critère de date à ajouter ?
			if (fromDate != null || toDate != null) {
				if (fromDate != null)
					pStmt.setString(idx++, sdf.format(fromDate.getTime()));
				if (toDate != null)
					pStmt.setString(idx++, sdf.format(toDate.getTime()));
			}
			// Exécution de le requête et extraction du résultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			long contributionSums = rs.getLong(1);
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return contributionSums;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					"Erreur lors du calcul du consommé d'un collaborateur sur un intervalle de temps donné",
					e);
		} finally {
			try {
				if (pStmt != null)
					pStmt.close();
			} catch (Throwable ignored) {
			}
		}
	}

	/**
	 * Calcule le nombre des contributions associée aux paramétres spécifiés.
	 * 
	 * <p>
	 * Tous les paramétres sont facultatifs. Chaque paramétre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramétre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.
	 * </p>
	 * 
	 * <p>
	 * En spécifiant la tache X, on connaîtra la somme des contribution pour la
	 * taches X. En ne spécifiant pas de tache, la somme sera effectuée quelque
	 * soit les tâches.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected long getContributionsNb(DbTransaction tx, Task task,
			Collaborator contributor, Integer year, Integer month, Integer day)
			throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		return getContributionsAggregation(tx,
				"count(ctb_duration)", task, contributor, year, month, day); //$NON-NLS-1$
	}

	/**
	 * Calcule le cumuls des consommations associees aux contributions pour les
	 * paramétres spécifiés.
	 * 
	 * <p>
	 * Tous les paramétres sont facultatifs. Chaque paramétre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramétre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.
	 * </p>
	 * 
	 * <p>
	 * En spécifiant la tache X, on connaîtra la somme des contribution pour la
	 * taches X. En ne spécifiant pas de tache, la somme sera effectuée quelque
	 * soit les tâches.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected long getContributionsSum(DbTransaction tx, Task task,
			Collaborator contributor, Integer year, Integer month, Integer day)
			throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		return getContributionsAggregation(tx,
				"sum(ctb_duration)", task, contributor, year, month, day); //$NON-NLS-1$
	}

	/**
	 * Calcule une aggregation associee aux contributions pour les paramétres
	 * spécifiés.
	 * 
	 * <p>
	 * Tous les paramétres sont facultatifs. Chaque paramétre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramétre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.
	 * </p>
	 * 
	 * <p>
	 * En spécifiant la tache X, on connaîtra la somme des contribution pour la
	 * taches X. En ne spécifiant pas de tache, la somme sera effectuée quelque
	 * soit les tâches.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param aggregation
	 *            la chaîne représentant l'aggrégation (ex:
	 *            <code>sum(ctb_contribution)</code>).
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private long getContributionsAggregation(DbTransaction tx,
			String aggregation, Task task, Collaborator contributor,
			Integer year, Integer month, Integer day) throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			StringBuffer baseRequest = new StringBuffer("select ") //$NON-NLS-1$
					.append(aggregation).append(
							" from CONTRIBUTION, TASK where ctb_task=tsk_id"); //$NON-NLS-1$
			// Cas ou la tache n'est pas spécifiée
			if (task == null) {
				// Préparation de la requête
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				completeContributionReqParams(pStmt, 1, contributor, year,
						month, day);
			}
			// Si la tache n'admet pas de sous-taches, le cumul de
			// budget, de consommé initial, de reste à faire sont
			// égaux à ceux de la tache
			else if (task.getSubTasksCount() == 0) {
				// Préparation de la requête
				baseRequest.append(" and tsk_id=?"); //$NON-NLS-1$
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				pStmt.setLong(1, task.getId());
				log.debug(" taskId=" + task.getId()); //$NON-NLS-1$
				completeContributionReqParams(pStmt, 2, contributor, year,
						month, day);
			}
			// Sinon, il faut calculer
			else {
				// Paramètre pour la clause 'LIKE'
				String pathLike = task.getFullPath() + "%"; //$NON-NLS-1$

				// Préparation de la requête
				baseRequest.append(" and tsk_path like ?"); //$NON-NLS-1$
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				pStmt.setString(1, pathLike);
				completeContributionReqParams(pStmt, 2, contributor, year,
						month, day);
			}

			// Exécution de la requête
			log.debug("Request : " + baseRequest); //$NON-NLS-1$
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			long agregation = rs.getLong(1);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			log.info("agregation=" + agregation); //$NON-NLS-1$
			return agregation;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTIONS_SUM_COMPUTATION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			try {
				if (pStmt != null)
					pStmt.close();
			} catch (Throwable ignored) {
			}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param durationId
	 *            l'identifiant de la durée.
	 * @return la durée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Duration getDuration(DbTransaction tx, long durationId)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("select dur_id, dur_is_active from DURATION where dur_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, durationId);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Reécupération du résultat
			Duration duration = null;
			if (rs.next())
				duration = rsToDuration(rs);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return duration;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.DURATION_SELECTION_BY_ID", new Long(durationId)), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param onlyActiveCollaborators
	 *            booléen indiquant si l'on ne doit retourner que les
	 *            collaborateurs actifs.
	 * @return la liste des durées.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Duration[] getDurations(DbTransaction tx,
			boolean onlyActiveCollaborators) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer(
					"select dur_id, dur_is_active from DURATION "); //$NON-NLS-1$
			if (onlyActiveCollaborators)
				request.append("where dur_is_active=?"); //$NON-NLS-1$
			request.append("order by dur_id asc"); //$NON-NLS-1$
			pStmt = tx.prepareStatement(request.toString());
			if (onlyActiveCollaborators)
				pStmt.setBoolean(1, true);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Duration> list = new ArrayList<Duration>();
			while (rs.next())
				list.add(rsToDuration(rs));

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return (Duration[]) list.toArray(new Duration[list.size()]);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.DURATIONS_SELECTION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Convertit le résultat d'une requête en durée.
	 * 
	 * @param rs
	 *            le result set.
	 * @return la durée.
	 * @throws SQLException
	 *             levé en cas de problème SQL.
	 */
	private Duration rsToDuration(ResultSet rs) throws SQLException {
		Duration duration = new Duration();
		duration.setId(rs.getLong(1));
		duration.setIsActive(rs.getBoolean(2));
		return duration;
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tache dont on veut connaitre la tache parent.
	 * @return la tache parent d'une tache spécifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task getParentTask(DbTransaction tx, Task task)
			throws DbException {
		Task parentTask = null;
		String parentTaskFullPath = task.getPath();
		// Si le chemin est vide, la tache parent est nulle (tache racine)
		if (parentTaskFullPath != null && !"".equals(parentTaskFullPath)) { //$NON-NLS-1$
			// Extraction du chemin et du numéro de la tache recherchée
			log.debug("Fullpath='" + parentTaskFullPath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			String path = parentTaskFullPath.substring(0,
					parentTaskFullPath.length() - 2);
			byte number = StringHelper.toByte(parentTaskFullPath
					.substring(parentTaskFullPath.length() - 2));
			log.debug(" => path=" + path); //$NON-NLS-1$
			log.debug(" => number=" + number); //$NON-NLS-1$

			// Recherche de la tache
			parentTask = getTask(tx, path, number);
		}
		// Retour du résultat
		return parentTask;
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param path
	 *            le chemin dont on veut connaître les taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task[] getTasks(DbTransaction tx, String path)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("select tsk_id, tsk_number from TASK where tsk_path=? order by tsk_number"); //$NON-NLS-1$
			pStmt.setString(1, path);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong(1));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			long[] taskIds = new long[list.size()];
			for (int i = 0; i < taskIds.length; i++) {
				taskIds[i] = list.get(i);
			}
			Task[] tasks = getTasks(tx, taskIds);
			return tasks;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_SELECTION_BY_PATH", path), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param parentTask
	 *            la tache parent dont on veut connaitre les sous-taches.
	 * @return la liste des sous-taches.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task[] getSubtasks(DbTransaction tx, Task parentTask)
			throws DbException {
		// Récupération du chemin à partir de la tache parent
		String fullpath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
		log.debug("Looking for tasks with path='" + fullpath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return getTasks(tx, fullpath);
	}

	/**
	 * Retourn la liste des taches correspondant au filtre de recherche
	 * spécifié.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param filter
	 *            le filtre de recherche.
	 * @return la liste des taches correspondant au filtre de recherche
	 *         spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task[] getTasks(DbTransaction tx, TaskSearchFilter filter)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer(
					"select tsk_id from TASK where "); //$NON-NLS-1$
			// Ajout du nom de champ
			switch (filter.getFieldIndex()) {
			case TaskSearchFilter.TASK_NAME_FIELD_IDX:
				request.append("tsk_name"); //$NON-NLS-1$
				break;
			case TaskSearchFilter.TASK_CODE_FIELD_IDX:
				request.append("tsk_code"); //$NON-NLS-1$
				break;
			default:
				throw new DbException(
						"Unknown field index '" + filter.getFieldIndex() + "'.", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// Ajout du critère de comparaison
			switch (filter.getCriteriaIndex()) {
			case TaskSearchFilter.IS_EQUAL_TO_CRITERIA_IDX:
				request.append("=?"); //$NON-NLS-1$
				break;
			case TaskSearchFilter.STARTS_WITH_CRITERIA_IDX:
			case TaskSearchFilter.ENDS_WITH_CRITERIA_IDX:
			case TaskSearchFilter.CONTAINS_CRITERIA_IDX:
				request.append(" like ?"); //$NON-NLS-1$
				break;
			default:
				throw new DbException(
						Strings.getString(
								"DbMgr.errors.UNKNOWN_CRITERIA_INDEX", new Integer(filter.getCriteriaIndex())), null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// Préparation de la requête
			log.debug("Search request : '" + request + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			pStmt = tx.prepareStatement(request.toString());
			String parameter = null;
			switch (filter.getCriteriaIndex()) {
			case TaskSearchFilter.IS_EQUAL_TO_CRITERIA_IDX:
				parameter = filter.getFieldValue();
				break;
			case TaskSearchFilter.STARTS_WITH_CRITERIA_IDX:
				parameter = filter.getFieldValue() + "%"; //$NON-NLS-1$
				break;
			case TaskSearchFilter.ENDS_WITH_CRITERIA_IDX:
				parameter = "%" + filter.getFieldValue(); //$NON-NLS-1$
				break;
			case TaskSearchFilter.CONTAINS_CRITERIA_IDX:
				parameter = "%" + filter.getFieldValue() + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				break;
			default:
				throw new DbException(
						"Unknown criteria index '" + filter.getCriteriaIndex() + "'.", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			log.debug("Search parameter : '" + parameter + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			pStmt.setString(1, parameter);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong(1));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Préparation du résultat
			long[] taskIds = new long[list.size()];
			for (int i = 0; i < taskIds.length; i++) {
				taskIds[i] = list.get(i);
			}
			Task[] tasks = getTasks(tx, taskIds);

			// On trie les taches manuellement car le tri base de données
			// pose un problème dans la mesure ou la BDD considère le champ
			// tsk_path comme numérique pour le tri ce qui pose un pb
			// Ex :
			// ROOT (path : 01)
			// +- T1 (path : 0101)
			// | +- T11 (path : 010101)
			// | +- T12 (path : 010102)
			// +- T2 (path : 0102)
			// Si on ramène l'ensemble des sous taches de ROOT, on voudrait
			// avoir
			// dans l'ordre T1, T11, T12, T2
			// Avec un tri base de donnée, on obtiendrait T1, T2, T11, T12 ; T2
			// ne se
			// trouve pas ou on l'attend, ceci en raison du fait qu'en
			// comparaison
			// numérique 0102 est < à 010101 et à 010102. Par contre, en
			// comparaison
			// de chaînes (en java), on a bien 0102 > 010101 et 010102.
			Arrays.sort(tasks, new Comparator<Task>() {
				public int compare(Task t1, Task t2) {
					return t1.getFullPath().compareTo(t2.getFullPath());
				}

			});

			// Retour du résultat
			return tasks;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASKS_SELECTION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param tasksIds
	 *            the task identifier.
	 * @return la tache dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task getTask(DbTransaction tx, long taskId)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Request preparation
			StringWriter request = prepareSelectTaskRequest();
			request.append(" theTask.tsk_id=?");
			completeSelectTaskRequest(request);
			pStmt = tx.prepareStatement(request.toString());
			pStmt.setLong(1, taskId);
			
			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			Task result = null;
			if (rs.next()) {
				result = toTask(rs);
			}
			
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_ID_FAILURE"), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param tasksIds
	 *            the task identifiers list.
	 * @return the tasks.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task[] getTasks(DbTransaction tx, long[] tasksIds)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		Map<Long, Task> tasksMap = new HashMap<Long, Task>();
		try {
			if (tasksIds != null && tasksIds.length != 0) {
				// The task id array is cut in sub arrays of maximum 250 tasks
				List<long[]> tasksIdsSubArrays = new ArrayList<long[]>();
				for (int i = 0; i < tasksIds.length; i += 250) {
					long[] subArray = new long[Math.min(250, tasksIds.length
							- i)];
					System.arraycopy(tasksIds, i, subArray, 0, subArray.length);
					tasksIdsSubArrays.add(subArray);
				}

				// Then a loop is performed over the sub arrays
				for (long[] tasksIdsSubArray : tasksIdsSubArrays) {
					// Préparation de la requête
					StringWriter request = prepareSelectTaskRequest();
					request.append(" theTask.tsk_id");
					if (tasksIdsSubArray.length == 1) {
						request.append("=?");
					} else {
						request.append(" in (");
						for (int i = 0; i < tasksIdsSubArray.length; i++) {
							request.append(i == 0 ? "?" : ", ?");
						}
						request.append(")");
					}
					completeSelectTaskRequest(request);
					pStmt = tx.prepareStatement(request.toString());
					for (int i = 0; i < tasksIdsSubArray.length; i++) {
						pStmt.setLong(i + 1, tasksIdsSubArray[i]);
					}

					// Exécution de la requête
					rs = pStmt.executeQuery();

					// Préparation du résultat
					while (rs.next()) {
						Task task = toTask(rs);
						tasksMap.put(task.getId(), task);
					}
					// Fermeture du ResultSet
					pStmt.close();
					pStmt = null;
				}
			}
			// The result must be reordonned in order to 
			// respect the task id array specified as an entry
			Task[] result = new Task[tasksIds.length];
			for (int i = 0; i < tasksIds.length; i++) {
				long taskId = tasksIds[i];
				result[i] = tasksMap.get(taskId);
			}
			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_ID_FAILURE"), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Prepares the SQL select request used to retrieve the tasks.
	 * 
	 * @return the SQL request.
	 */
	private StringWriter prepareSelectTaskRequest() {
		StringWriter request = new StringWriter();
		request.append("select theTask.tsk_id, theTask.tsk_path, theTask.tsk_number, theTask.tsk_code, theTask.tsk_name, theTask.tsk_budget, theTask.tsk_initial_cons, theTask.tsk_todo, theTask.tsk_comment, count(subTask.tsk_id)"
				+ " from TASK as theTask left join TASK as subTask on subTask.tsk_path = concat(theTask.tsk_path, theTask.tsk_number)"
				+ " where");
		return request;
	}

	/**
	 * Completes the SQL select request used to retrieve the tasks.
	 * 
	 * @param request
	 *            the request.
	 */
	private void completeSelectTaskRequest(StringWriter request) {
		// HSQLDB expects all the selected columns in the group by
		// directive
		request.append(" group by theTask.tsk_id, theTask.tsk_path, theTask.tsk_number, theTask.tsk_code, theTask.tsk_name, theTask.tsk_budget, theTask.tsk_initial_cons, theTask.tsk_todo, theTask.tsk_comment"); //$NON-NLS-1$
	}

	/**
	 * Builds a Task from a SQL result set.
	 * 
	 * @param rs
	 *            the result set.
	 * @return the task.
	 * @throws SQLException
	 *             thrown if an error occurrs.
	 */
	protected Task toTask(ResultSet rs) throws SQLException {
		Task task = null;
		Long tskId = rs.getLong(1);
		// If the task id that is returned is null,
		// it means that the request returned nothing
		// (the task id is 'not null' in the database)
		if (tskId != null) {
			task = new Task();
			task.setId(tskId);
			task.setPath(rs.getString(2));
			task.setNumber(StringHelper.toByte(rs.getString(3)));
			task.setCode(rs.getString(4));
			task.setName(rs.getString(5));
			task.setBudget(rs.getLong(6));
			task.setInitiallyConsumed(rs.getLong(7));
			task.setTodo(rs.getLong(8));
			task.setComment(rs.getString(9));
			task.setSubTasksCount(rs.getInt(10));
		}
		return task;
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskNumber
	 *            le numéro de la tache recherchée.
	 * @return la tache dont le chemin et le numéro sont spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task getTask(DbTransaction tx, String taskPath,
			byte taskNumber) throws DbException {
		log.debug("getTask(" + taskPath + ", " + taskNumber + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringWriter request = prepareSelectTaskRequest();
			request.append(" theTask.tsk_path=? and theTask.tsk_number=?");
			completeSelectTaskRequest(request);
			pStmt = tx.prepareStatement(request.toString());
			pStmt.setString(1, taskPath);
			pStmt.setString(2, StringHelper.toHex(taskNumber));

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			Task task = null;
			if (rs.next()) {
				task = toTask(rs);
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			log.debug("task = " + task); //$NON-NLS-1$
			return task;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.TASK_SELECTION_BY_NUMBER_FROM_PATH_FAILURE", new Byte(taskNumber), taskPath), e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskCode
	 *            le code de la tache recherchée.
	 * @return la tache dont le code et la tache parent sont spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task getTask(DbTransaction tx, String taskPath,
			String taskCode) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringWriter request = prepareSelectTaskRequest();
			request.append(" theTask.tsk_path=? and theTask.tsk_code=?"); //$NON-NLS-1$
			completeSelectTaskRequest(request);
			pStmt = tx.prepareStatement(request.toString());
			pStmt.setString(1, taskPath);
			pStmt.setString(2, taskCode);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			Task task = null;
			if (rs.next()) {
				task = toTask(rs);
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return task;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_SELECTION_BY_CODE_FAILURE", taskCode), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param collaborator
	 *            le collaborateur.
	 * @param fromDate
	 *            date de début.
	 * @param toDate
	 *            date de fin.
	 * @return la liste de taches associées au collaborateur entre les 2 dates
	 *         spécifiées.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task[] getTasks(DbTransaction tx,
			Collaborator collaborator, Calendar fromDate, Calendar toDate)
			throws DbException {
		log.debug("getTasks(" + collaborator + ", " + sdf.format(fromDate.getTime()) + ", " + sdf.format(toDate.getTime()) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("select distinct ctb_task, tsk_path, tsk_number from CONTRIBUTION, TASK where ctb_task=tsk_id and ctb_contributor=? and ctb_year*10000 + ( ctb_month*100 + ctb_day ) between ? and ? order by tsk_path, tsk_number"); //$NON-NLS-1$
			pStmt.setLong(1, collaborator.getId());
			pStmt.setString(2, sdf.format(fromDate.getTime()));
			pStmt.setString(3, sdf.format(toDate.getTime()));

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong(1));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			long[] taskIds = new long[list.size()];
			for (int i = 0; i < taskIds.length; i++) {
				taskIds[i] = list.get(i);
			}
			return getTasks(tx, taskIds);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_COLLABORATOR_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tâche pour laquelle on souhaite connaître les totaux.
	 * @param fromDate
	 *            date de départ à prendre en compte pour le calcul.
	 * @param toDate
	 *            date de fin à prendre en compte pour le calcul.
	 * @return les totaux associés à une tache (consommé, etc.).
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected TaskSums getTaskSums(DbTransaction tx, Task task,
			Calendar fromDate, Calendar toDate) throws DbException {
		// TODO Factoriser cette méthode avec getContributionsSum
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation du résultat
			TaskSums taskSums = new TaskSums();
			boolean taskIsLeaf = task != null && task.getSubTasksCount() == 0;

			/**
			 * Calcul de la partie indépendante des contributions (budget /
			 * consommation initiale / reste à faire
			 */

			// Si la tache n'admet pas de sous-taches, le cumul de
			// budget, de consommé initial, de reste à faire sont
			// égaux à ceux de la tache
			if (taskIsLeaf) {
				taskSums.setBudgetSum(task.getBudget());
				taskSums.setInitiallyConsumedSum(task.getInitiallyConsumed());
				taskSums.setTodoSum(task.getTodo());
			}
			// Sinon, il faut calculer
			else {
				// Calcul des cumuls
				pStmt = tx
						.prepareStatement("select sum(tsk_budget), sum(tsk_initial_cons), sum(tsk_todo) from TASK where tsk_path like ?"); //$NON-NLS-1$
				pStmt.setString(1, (task == null ? "" : task.getFullPath())
						+ "%");
				rs = pStmt.executeQuery();
				if (!rs.next())
					throw new DbException(
							Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
				taskSums.setBudgetSum(rs.getLong(1));
				taskSums.setInitiallyConsumedSum(rs.getLong(2));
				taskSums.setTodoSum(rs.getLong(3));
				pStmt.close();
				pStmt = null;
			}

			/**
			 * Calcul du consommé
			 */

			// Préparation de la requête
			StringBuffer request = new StringBuffer(
					"select sum(ctb_duration), count(ctb_duration) from CONTRIBUTION, TASK where ctb_task=tsk_id and ");
			// En fonction du cas, on recherche soit sur la tache précise
			// (tsk_id=?), soit sur un arbre (tsk_path like ?)
			request.append(taskIsLeaf ? "tsk_id=?" : "tsk_path like ?");
			// Y'a-t-il un critère de date à ajouter ?
			if (fromDate != null || toDate != null) {
				request.append(" and ( ctb_year*10000 + ( ctb_month*100 + ctb_day ) )");
				// Deux dates spécifiées
				if (fromDate != null && toDate != null)
					request.append(" between ? and ?");
				// Seule la date de début spécifiée
				else if (fromDate != null)
					request.append(">=?");
				// Seule la date de fin spécifiée
				else
					request.append("<=?");
			}

			// Calcul du consommé
			pStmt = tx.prepareStatement(request.toString()); //$NON-NLS-1$
			// En fonction du cas, on recherche soit sur la tache précise
			// (tsk_id=?), soit sur un arbre (tsk_path like ?)
			if (taskIsLeaf)
				pStmt.setLong(1, task.getId());
			else
				pStmt.setString(1, (task == null ? "" : task.getFullPath())
						+ "%");
			// Y'a-t-il un critère de date à ajouter ?
			if (fromDate != null || toDate != null) {
				int i = 2;
				if (fromDate != null)
					pStmt.setString(i++, sdf.format(fromDate.getTime()));
				if (toDate != null)
					pStmt.setString(i++, sdf.format(toDate.getTime()));
			}
			// Exécution de le requête et extraction du résultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			taskSums.setConsumedSum(rs.getLong(1));
			taskSums.setContributionsNb(rs.getLong(2));
			pStmt.close();
			pStmt = null;

			/**
			 * Si un critère de date de fin a été spécifié, il faut corriger le
			 * RAF calculé plus haut sinon on ne tient pas compte des
			 * consommations au dela de cette date. En effet RAF à une date
			 * donnée = RAF identifié au niveau de la tache + consommations
			 * futures déja enregistrées dans le système
			 */
			if (toDate != null) {
				request = new StringBuffer(
						"select sum(ctb_duration) from CONTRIBUTION, TASK where ctb_task=tsk_id and ");
				request.append(taskIsLeaf ? "tsk_id=?" : "tsk_path like ?");
				request.append(" and ( ctb_year*10000 + ( ctb_month*100 + ctb_day ) ) > ?");
				// Calcul des consommations au delà de la date de fin spécifiée
				pStmt = tx.prepareStatement(request.toString()); //$NON-NLS-1$
				if (taskIsLeaf)
					pStmt.setLong(1, task.getId());
				else
					pStmt.setString(1, (task == null ? "" : task.getFullPath())
							+ "%");
				pStmt.setString(2, sdf.format(toDate.getTime()));
				// Exécution de le requête et extraction du résultat
				rs = pStmt.executeQuery();
				if (!rs.next())
					throw new DbException(
							Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
				// Mise à jour du RAF
				taskSums.setTodoSum(taskSums.getTodoSum() + rs.getLong(1));
				pStmt.close();
				pStmt = null;
			}

			/**
			 * Retour du résultat
			 */

			// Retour du résultat
			return taskSums;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.TASK_SUMS_COMPUTATION_FAILURE", new Long(task.getId())), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			try {
				if (pStmt != null)
					pStmt.close();
			} catch (Throwable ignored) {
			}
		}
	}

	/**
	 * Supprime un collaborateur.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param collaborator
	 *            le collaborateur à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void removeCollaborator(DbTransaction tx,
			Collaborator collaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("delete from COLLABORATOR where clb_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, collaborator.getId());

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_ROW_DELETION_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_DELETION_FAILURE", collaborator.getLogin()), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Supprime une contribution.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param contribution
	 *            la contribution à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void removeContribution(DbTransaction tx,
			Contribution contribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("delete from CONTRIBUTION where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
			pStmt.setInt(1, contribution.getYear());
			pStmt.setInt(2, contribution.getMonth());
			pStmt.setInt(3, contribution.getDay());
			pStmt.setLong(4, contribution.getContributorId());
			pStmt.setLong(5, contribution.getTaskId());

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_DISCONNECTION_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTION_DELETION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Supprime une durée du référentiel de durées.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la durée à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void removeDuration(DbTransaction tx, Duration duration)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx.prepareStatement("delete from DURATION where dur_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, duration.getId());

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_ROW_DELETION_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.DURATION_DELETION_FAILURE", new Long(duration.getId())), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Supprime une tache.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tache à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void removeTask(DbTransaction tx, Task task)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Control sur les sous taches
			Task[] subTasks = getSubtasks(tx, task);
			for (int i = 0; i < subTasks.length; i++) {
				removeTask(tx, subTasks[i]);
			}

			// Préparation de la requête
			pStmt = tx.prepareStatement("delete from TASK where tsk_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, task.getId());

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed != 1)
				throw new SQLException("No row was deleted"); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_DELETION_FAILURE", task.getName()), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Annule le modifications effectuées dans le cadre d'une transactrion.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected void rollbackTransaction(DbTransaction tx)
			throws DbException {
		try {
			tx.getConnection().rollback();
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_ROLLBACK_FAILURE"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Modifie les attributs d'un collaborateur.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param collaborator
	 *            le collaborateur à modifier.
	 * @return le collaborateur modifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Collaborator updateCollaborator(DbTransaction tx,
			Collaborator collaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("update COLLABORATOR set clb_login=?, clb_first_name=?, clb_last_name=?, clb_is_active=? where clb_id=?"); //$NON-NLS-1$
			pStmt.setString(1, collaborator.getLogin());
			pStmt.setString(2, collaborator.getFirstName());
			pStmt.setString(3, collaborator.getLastName());
			pStmt.setBoolean(4, collaborator.getIsActive());
			pStmt.setLong(5, collaborator.getId());

			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return collaborator;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_UPDATE_FAILURE", collaborator.getLogin()), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Modifie les attributs d'une contribution.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param contribution
	 *            la contribution à modifier.
	 * @return la contribution modifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Contribution updateContribution(DbTransaction tx,
			Contribution contribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("update CONTRIBUTION set ctb_duration=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
			pStmt.setLong(1, contribution.getDurationId());
			pStmt.setInt(2, contribution.getYear());
			pStmt.setInt(3, contribution.getMonth());
			pStmt.setInt(4, contribution.getDay());
			pStmt.setLong(5, contribution.getContributorId());
			pStmt.setLong(6, contribution.getTaskId());

			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return contribution;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTION_UPDATE_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Met à jour une durée.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la durée à mettre à jour.
	 * @return la durée mise à jour.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Duration updateDuration(DbTransaction tx, Duration duration)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("update DURATION set dur_is_active=? where dur_id=?"); //$NON-NLS-1$
			pStmt.setBoolean(1, duration.getIsActive());
			pStmt.setLong(2, duration.getId());

			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return duration;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.DURATION_UPDATE_FAILURE", new Long(duration.getId())), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Change la tache d'une contribution.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param contribution
	 *            la contribution.
	 * @param newContributionTask
	 *            la tache à affecter.
	 * @return la contribution mise à jour.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Contribution changeContributionTask(DbTransaction tx,
			Contribution contribution, Task newContributionTask)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("update CONTRIBUTION set ctb_task=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
			pStmt.setLong(1, newContributionTask.getId());
			pStmt.setInt(2, contribution.getYear());
			pStmt.setInt(3, contribution.getMonth());
			pStmt.setInt(4, contribution.getDay());
			pStmt.setLong(5, contribution.getContributorId());
			pStmt.setLong(6, contribution.getTaskId());

			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Mise à jour de la contribution
			contribution.setTaskId(newContributionTask.getId());

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return contribution;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTION_UPDATE_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Modifie les attributs d'une tache.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param task
	 *            la tache à modifier.
	 * @return la tache modifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected Task updateTask(DbTransaction tx, Task task)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx
					.prepareStatement("update TASK set tsk_path=?, tsk_number=?, tsk_code=?, tsk_name=?, tsk_budget=?, tsk_initial_cons=?, tsk_todo=?, tsk_comment=? where tsk_id=?"); //$NON-NLS-1$
			pStmt.setString(1, task.getPath());
			pStmt.setString(2, StringHelper.toHex(task.getNumber()));
			pStmt.setString(3, task.getCode());
			pStmt.setString(4, task.getName());
			pStmt.setLong(5, task.getBudget());
			pStmt.setLong(6, task.getInitiallyConsumed());
			pStmt.setLong(7, task.getTodo());
			pStmt.setString(8, task.getComment());
			pStmt.setLong(9, task.getId());

			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return task;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_UPDATE_FAILURE", task.getName()), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Complete la requete de calcul de la somme des contributions.
	 * 
	 * @param requestBase
	 *            buffer utilisé pour la construction de la requête.
	 * @param contributor
	 *            le collaborateur ayant contribué à la tache (facultatif).
	 * @param year
	 *            l'année (facultative)
	 * @param month
	 *            le mois (facultatif)
	 * @param day
	 *            le jour (facultatif)
	 */
	private void completeContributionRequest(StringBuffer requestBase,
			Collaborator contributor, Integer year, Integer month, Integer day) {
		if (contributor != null)
			requestBase.append(" and ctb_contributor=?"); //$NON-NLS-1$
		if (year != null)
			requestBase.append(" and ctb_year=?"); //$NON-NLS-1$
		if (month != null)
			requestBase.append(" and ctb_month=?"); //$NON-NLS-1$
		if (day != null)
			requestBase.append(" and ctb_day=?"); //$NON-NLS-1$
		log.debug("built request : " + requestBase.toString()); //$NON-NLS-1$
	}

	/**
	 * Complete les paramétres de la requete de calcul de la somme des
	 * contributions.
	 * 
	 * @param pStmt
	 *            le statement.
	 * @param startIndex
	 * @param contributor
	 *            le collaborateur ayant contribué à la tache (facultatif).
	 * @param year
	 *            l'année (facultative)
	 * @param month
	 *            le mois (facultatif)
	 * @param day
	 *            le jour (facultatif)
	 * @throws SQLException
	 *             levé en cas d'incident avec la base de données.
	 */
	private void completeContributionReqParams(PreparedStatement pStmt,
			int startIndex, Collaborator contributor, Integer year,
			Integer month, Integer day) throws SQLException {
		int idx = startIndex;
		log.debug("contributorId=" + (contributor != null ? String.valueOf(contributor.getId()) : "null")); //$NON-NLS-1$ //$NON-NLS-2$
		log.debug("year=" + year); //$NON-NLS-1$
		log.debug("month=" + month); //$NON-NLS-1$
		log.debug("day=" + day); //$NON-NLS-1$
		if (contributor != null)
			pStmt.setLong(idx++, contributor.getId());
		if (year != null)
			pStmt.setInt(idx++, year.intValue());
		if (month != null)
			pStmt.setInt(idx++, month.intValue());
		if (day != null)
			pStmt.setInt(idx++, day.intValue());
	}

	/**
	 * Génère un nouveau numéro de tache pour un chemin donné.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param path
	 *            le chemin considéré.
	 * @return le numéro généré.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	protected byte newTaskNumber(DbTransaction tx, String path)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Recherche du max
			pStmt = tx
					.prepareStatement("select max(tsk_number) from TASK where tsk_path=?"); //$NON-NLS-1$
			pStmt.setString(1, path);
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			String maxStr = rs.getString(1);
			byte max = maxStr != null ? StringHelper.toByte(maxStr) : 0;
			log.debug("  => max= : " + max); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return (byte) (max + 1);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_NUMBER_COMPUTATION_FAILURE", path), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (pStmt != null)
				try {
					pStmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Retourne l'identifiant généré automatiquement par la base de données.
	 * 
	 * @param pStmt
	 *            le statement SQL.
	 * @return l'identifiant généré.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private long getGeneratedId(PreparedStatement pStmt)
			throws DbException {
		long generatedId = -1;
		PreparedStatement pStmt1 = null;
		try {
			// Récupération de la connexion
			Connection con = pStmt.getConnection();
			// Cas de HSQLDB
			if (isHSQLDB(con)) {
				log.debug("HSQL Database detected"); //$NON-NLS-1$
				pStmt1 = con.prepareStatement("call identity()"); //$NON-NLS-1$
				ResultSet rs = pStmt1.executeQuery();
				if (!rs.next())
					throw new DbException(
							Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
				generatedId = rs.getLong(1);

				// Fermeture du statement
				pStmt1.close();
				pStmt1 = null;
			} else {
				log.debug("Generic Database detected"); //$NON-NLS-1$
				// Récupération de l'identifiant généré
				ResultSet rs = pStmt.getGeneratedKeys();
				if (!rs.next())
					throw new DbException(
							Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
				generatedId = rs.getLong(1);
			}
			// Retour du résultat
			log.debug("Generated id=" + generatedId); //$NON-NLS-1$
			return generatedId;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_AUTOINCREMENT_FAILURE"), e); //$NON-NLS-1$
		} finally {
			if (pStmt1 != null)
				try {
					pStmt1.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Indique si la BDD de données est une base HSQLDB.
	 * 
	 * @param con
	 *            la connexion SQL.
	 * @return un booléen indiquant si la BDD est de type HSQLDB.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private boolean isHSQLDB(Connection con) throws DbException {
		try {
			// Récupération du nom de la base de données
			String dbName = con.getMetaData().getDatabaseProductName();
			log.debug("DbName=" + dbName); //$NON-NLS-1$
			return "HSQL Database Engine".equals(dbName); //$NON-NLS-1$
		} catch (SQLException e) {
			log.info("SQL Error", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_DATABASE_NAME_EXTRACTION_FAILURE"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Indique si la BDD de données est une base HSQLDB embarquée.
	 * 
	 * @param con
	 *            la connexion SQL.
	 * @return un booléen indiquant si la BDD est de type HSQLDB embarquée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private boolean isEmbeddedHSQLDB(Connection con) throws DbException {
		try {
			// Récupération du nom de la base de données
			String dbName = con.getMetaData().getDatabaseProductName();
			log.debug("DbName=" + dbName); //$NON-NLS-1$
			return isHSQLDB(con) && ds.getUrl().startsWith("jdbc:hsqldb:file"); //$NON-NLS-1$
		} catch (SQLException e) {
			log.info("SQL Error", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_DATABASE_NAME_EXTRACTION_FAILURE"), e); //$NON-NLS-1$
		}
	}

}
