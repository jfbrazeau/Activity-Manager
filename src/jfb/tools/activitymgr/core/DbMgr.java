/*
 * Copyright (c) 2004-2010, Jean-Fran�ois Brazeau. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
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

import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Duration;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.beans.TaskSearchFilter;
import jfb.tools.activitymgr.core.beans.TaskSums;
import jfb.tools.activitymgr.core.util.StringHelper;
import jfb.tools.activitymgr.core.util.Strings;

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
	private static BasicDataSource ds = null;

	/**
	 * Contexte de thread utilis� pour d�tecter les anomalies associ�es � la
	 * gestion de transaction
	 */
	private static ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();

	/**
	 * Initialise la connexion � la base de donn�es.
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
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void initDatabaseAccess(String driverName, String url,
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

			// Tentative de r�cup�ration d'une connexion
			// pour d�tecter les probl�mes de connexion
			Connection con = newDs.getConnection();
			con.close();

			// Sauvegarde de la r�f�rence
			ds = newDs;
		} catch (SQLException e) {
			log.info("SQL Exception", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_CONNECTION_OPEN"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Ferme la base de donn�es.
	 * 
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la BDD.
	 */
	protected static void closeDatabaseAccess() throws DbException {
		try {
			if (ds != null) {
				// R�cup�ration de la connexion
				Connection con = ds.getConnection();

				// Cas d'une base HSQLDB embarqu�e
				if (isEmbeddedHSQLDB(con)) {
					// Extinction de la base de donn�es
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
	 * Une connexion � la base de donn�es est �tablie. Celle ci doit �tre
	 * valid�e par la couche appelante par une invocation de
	 * <code>endTransaction</code>.
	 * </p>
	 * 
	 * @return le contexte de transaction.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static DbTransaction beginTransaction() throws DbException {
		try {
			// Est-on connect� � la BDD ?
			if (ds == null)
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_CONNECTION_ESTABLISHMENT_FAILURE"), null); //$NON-NLS-1$
			// Obtention d'une connexion
			Connection con = ds.getConnection();
			if (threadLocal.get() != null)
				throw new Error(
						Strings.getString("DbMgr.errors.SQL_MULTI_TRANSACTION_DETECTED")); //$NON-NLS-1$
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
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void commitTransaction(DbTransaction tx)
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
	 * V�rifie si les tables existent dans le mod�le.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @return un bool�en indiquant si la table sp�cifi�e existe dans le mod�le.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static boolean tablesExist(DbTransaction tx) throws DbException {
		boolean tablesExist = true;
		tablesExist &= tableExists(tx, "COLLABORATOR"); //$NON-NLS-1$
		tablesExist &= tableExists(tx, "CONTRIBUTION"); //$NON-NLS-1$
		tablesExist &= tableExists(tx, "DURATION"); //$NON-NLS-1$
		tablesExist &= tableExists(tx, "TASK"); //$NON-NLS-1$
		return tablesExist;
	}

	/**
	 * V�rifie si une table existe dans le mod�le.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param tableName
	 *            le nom de la table.
	 * @return un bool�en indiquant si la table sp�cifi�e existe dans le mod�le.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	private static boolean tableExists(DbTransaction tx, String tableName)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// R�cup�ration de la connexion
			Connection con = tx.getConnection();

			// Recherche de la table
			ResultSet rs = con.getMetaData().getTables(null, null, tableName,
					new String[] { "TABLE" }); //$NON-NLS-1$

			// R�cup�ration du r�sultat
			boolean exists = rs.next();

			// Retour du r�sultat
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
	 * Cr�e les tables du mod�le de donn�es.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void createTables(DbTransaction tx) throws DbException {
		Statement stmt = null;
		try {
			// R�cup�ration de la connexion
			Connection con = tx.getConnection();

			// Lecture du fichier SQL de cr�ation de la BDD
			String batchName = "sql/" + (isHSQLDB(con) ? "hsqldb.sql" : "mysqldb.sql"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			InputStream in = DbMgr.class.getResourceAsStream(batchName);
			String batchContent = null;
			try {
				batchContent = StringHelper.fromInputStream(in);
			} catch (IOException e) {
				log.info(
						"I/O error while loading table creation SQL script.", e); //$NON-NLS-1$
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_SCRIPT_LOAD_FAILURE"), null); //$NON-NLS-1$
			}

			// D�coupage et ex�cution du batch
			stmt = con.createStatement();
			// TODO Externaliser le d�coupage du script SQL
			LineNumberReader lnr = new LineNumberReader(new StringReader(
					batchContent));
			StringBuffer buf = new StringBuffer();
			boolean proceed = true;
			do {
				String line = null;
				// On ne lit dans le flux que si la ligne courante n'est pas
				// encore totalement trait�e
				if (line == null) {
					try {
						line = lnr.readLine();
					} catch (IOException e) {
						log.info(
								"Unexpected I/O error while reading memory stream!", e); //$NON-NLS-1$
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
						// Sinon on regarde si la ligne poss�de
						// un point virgule
						int idx = line.indexOf(';');
						// Si c'est le cas, on d�coupe la cha�ne et on
						// ex�cute la requ�te
						if (idx >= 0) {
							buf.append(line.subSequence(0, idx));
							line = line.substring(idx);
							String sql = buf.toString();
							buf.setLength(0);
							log.debug(" - sql='" + sql + "'"); //$NON-NLS-1$ //$NON-NLS-2$
							if (!"".equals(sql)) //$NON-NLS-1$
								stmt.executeUpdate(sql);
						}
						// sinon on ajoute la ligne au buffer de reque�te
						else {
							buf.append(line);
							buf.append('\n');
						}
					}
				}

			} while (proceed);

			// Test de l'existence des tables
			if (!tablesExist(tx))
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_TABLE_CREATION_FAILURE"), null); //$NON-NLS-1$

			// Fermeture du statement
			stmt.close();
			stmt = null;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException("Database table creation failure", e); //$NON-NLS-1$
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/**
	 * Cr�e un collaborateur.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param newCollaborator
	 *            le collaborateur � cr�er.
	 * @return le collaborateur apr�s cr�ation.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Collaborator createCollaborator(DbTransaction tx,
			Collaborator newCollaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("insert into collaborator (clb_login, clb_first_name, clb_last_name, clb_is_active) values (?, ?, ?, ?)"); //$NON-NLS-1$
			pStmt.setString(1, newCollaborator.getLogin());
			pStmt.setString(2, newCollaborator.getFirstName());
			pStmt.setString(3, newCollaborator.getLastName());
			pStmt.setBoolean(4, newCollaborator.getIsActive());
			pStmt.executeUpdate();

			// R�cup�ration de l'identifiant g�n�r�
			long generatedId = getGeneratedId(pStmt);
			log.debug("Generated id=" + generatedId); //$NON-NLS-1$
			newCollaborator.setId(generatedId);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Cr�e une contribution.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param newContribution
	 *            la nouvelle contribution.
	 * @return la contribution apr�s cr�ation.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Contribution createContribution(DbTransaction tx,
			Contribution newContribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("insert into contribution (ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration) values (?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$
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

			// Retour du r�sultat
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
	 * Cr�e une contribution.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param newDuration
	 *            la nouvelle dur�e.
	 * @return la dur�e apr�s cr�ation.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Duration createDuration(DbTransaction tx,
			Duration newDuration) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("insert into duration (dur_id, dur_is_active) values (?, ?)"); //$NON-NLS-1$
			pStmt.setLong(1, newDuration.getId());
			pStmt.setBoolean(2, newDuration.getIsActive());
			pStmt.executeUpdate();

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Cr�e une tache.
	 * 
	 * <p>
	 * La tache parent peut �tre nulle pour indiquer que la nouvelle tache est
	 * une tache racine.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param parentTask
	 *            la tache parent accueillant la nouvelle tache.
	 * @param newTask
	 *            la nouvelle tache.
	 * @return la tache apr�s cr�ation.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task createTask(DbTransaction tx, Task parentTask,
			Task newTask) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Mise � jour du chemin de la t�che
			String parentPath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
			newTask.setPath(parentPath);

			// G�n�ration du num�ro de la t�che
			byte taskNumber = newTaskNumber(tx, parentPath);
			newTask.setNumber(taskNumber);

			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("insert into task (tsk_path, tsk_number, tsk_code, tsk_name, tsk_budget, tsk_initial_cons, tsk_todo, tsk_comment) values (?, ?, ?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$
			pStmt.setString(1, newTask.getPath());
			pStmt.setByte(2, newTask.getNumber());
			pStmt.setString(3, newTask.getCode());
			pStmt.setString(4, newTask.getName());
			pStmt.setLong(5, newTask.getBudget());
			pStmt.setLong(6, newTask.getInitiallyConsumed());
			pStmt.setLong(7, newTask.getTodo());
			pStmt.setString(8, newTask.getComment());
			pStmt.executeUpdate();

			// R�cup�ration de l'identifiant g�n�r�
			long generatedId = getGeneratedId(pStmt);
			log.debug("Generated id=" + generatedId); //$NON-NLS-1$
			newTask.setId(generatedId);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * V�rifie si la dur�e est utilis�e en base.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la dur�e � v�rifier.
	 * @return un bool�en indiquant si la dur�e est utilis�e.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static boolean durationIsUsed(DbTransaction tx, Duration duration)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select count(*) from contribution where ctb_duration=?"); //$NON-NLS-1$
			pStmt.setLong(1, duration.getId());

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Pr�paration du r�sultat
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			boolean durationIsUsed = rs.getInt(1) > 0;

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void endTransaction(DbTransaction tx) throws DbException {
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
	 *            l'identifiant du collaborateur recherch�.
	 * @return le collaborateur dont l'identifiant est sp�cifi�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Collaborator getCollaborator(DbTransaction tx,
			long collaboratorId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = tx
					.prepareStatement("select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from collaborator where clb_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, collaboratorId);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Pr�paration du r�sultat
			Collaborator collaborator = null;
			if (rs.next())
				collaborator = rsToCollaborator(rs);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Convertit le r�sultat d'une requ�te en collaborateur.
	 * 
	 * @param rs
	 *            le result set.
	 * @return le collaborateur.
	 * @throws SQLException
	 *             lev� en cas de probl�me SQL.
	 */
	private static Collaborator rsToCollaborator(ResultSet rs)
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
	 *            l'identifiant de connexion du collaborateur recherch�.
	 * @return le collaborateur dont l'identifiant de connexion est sp�cifi�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Collaborator getCollaborator(DbTransaction tx, String login)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from collaborator where clb_login=?"); //$NON-NLS-1$
			pStmt.setString(1, login);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Pr�paration du r�sultat
			Collaborator collaborator = null;
			if (rs.next())
				collaborator = rsToCollaborator(rs);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            index de l'attribut utilis� pour le tri.
	 * @param ascendantSort
	 *            bool�en indiquant si le tri doit �tre ascendant.
	 * @param onlyActiveCollaborators
	 *            bool�en indiquant si l'on ne doit retourner que les
	 *            collaborateurs actifs.
	 * @return la liste des collaborateurs.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Collaborator[] getCollaborators(DbTransaction tx,
			int orderByClauseFieldIndex, boolean ascendantSort,
			boolean onlyActiveCollaborators) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			StringBuffer request = new StringBuffer(
					"select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from collaborator "); //$NON-NLS-1$
			if (onlyActiveCollaborators)
				request.append("where clb_is_active=?"); //$NON-NLS-1$
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

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Collaborator> list = new ArrayList<Collaborator>();
			while (rs.next())
				list.add(rsToCollaborator(rs));

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            le collaborateur associ� aux contributions.
	 * @param task
	 *            la tache associ�e aux contributions.
	 * @param fromDate
	 *            la date de d�part.
	 * @param toDate
	 *            la date de fin.
	 * @return la liste des contributions associ�es aux param�tres sp�cifi�s.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Contribution[] getContributions(DbTransaction tx,
			Collaborator contributor, Task task, Calendar fromDate,
			Calendar toDate) throws DbException {
		log.debug("getContributions(" + contributor + ", " + task + ", " + sdf.format(fromDate.getTime()) + ", " + sdf.format(toDate.getTime()) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Conversion des dates
			String fromDateStr = sdf.format(fromDate.getTime());
			String toDateStr = sdf.format(toDate.getTime());

			// Pr�paration de la requ�te
			// 1� cas : les deux dates sont diff�rentes
			if (!fromDateStr.equals(toDateStr)) {
				pStmt = tx
						.prepareStatement("select ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration from contribution where ctb_contributor=? and ctb_task=? and ctb_year*10000 + ( ctb_month*100 + ctb_day ) between ? and ?"); //$NON-NLS-1$
				pStmt.setLong(1, contributor.getId());
				pStmt.setLong(2, task.getId());
				pStmt.setString(3, fromDateStr);
				pStmt.setString(4, toDateStr);
			}
			// 2� cas : les deux dates sont �gales
			else {
				pStmt = tx
						.prepareStatement("select ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration from contribution where ctb_contributor=? and ctb_task=? and ctb_year*10000 + ( ctb_month*100 + ctb_day ) = ?"); //$NON-NLS-1$
				pStmt.setLong(1, contributor.getId());
				pStmt.setLong(2, task.getId());
				pStmt.setString(3, fromDateStr);
			}

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Extraction du r�sultat
			Contribution[] result = rsToContributions(rs);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Extrait les contributions du resultat de la requ�te SQL.
	 * 
	 * @param rs
	 *            le r�sultat de la requ�te SQL.
	 * @return les contributions extraites.
	 * @throws SQLException
	 *             lev� en cas d'incident avec la base de donn�es.
	 */
	private static Contribution[] rsToContributions(ResultSet rs)
			throws SQLException {
		// Recherche des sous-taches
		ArrayList<Contribution> list = new ArrayList<Contribution>();
		while (rs.next()) {
			// Pr�paration du r�sultat
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
	 * Retourne les contributions associ�es aux param�tres sp�cifi�s.
	 * 
	 * <p>
	 * Tous les param�tres sont facultatifs. Chaque param�tre sp�cifi� ag�t
	 * comme un filtre sur le r�sultat. A l'inverse, l'omission d'un param�tre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut consid�r�.
	 * </p>
	 * 
	 * <p>
	 * La sp�cification des param�tres r�pond aux m�mes r�gles que pour la
	 * m�thode <code>getContributionsSum</code>.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la t�che associ�e aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associ� aux contributions (facultatif).
	 * @param year
	 *            l'ann�e (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return les contributions.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgr#getContributionsSum(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	protected static Contribution[] getContributions(DbTransaction tx,
			Task task, Collaborator contributor, Integer year, Integer month,
			Integer day) throws DbException {
		log.debug("getContributions(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			StringBuffer baseRequest = new StringBuffer(
					"select ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration from contribution, task where ctb_task=tsk_id"); //$NON-NLS-1$
			String orderByClause = " order by ctb_year, ctb_month, ctb_day, tsk_path, tsk_number, ctb_contributor, ctb_duration"; //$NON-NLS-1$
			// Cas ou la tache n'est pas sp�cifi�e
			if (task == null) {
				// Pr�paration de la requ�te
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				baseRequest.append(orderByClause);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				completeContributionReqParams(pStmt, 1, contributor, year,
						month, day);
			}
			// Si la tache n'admet pas de sous-taches, le cumul de
			// budget, de consomm� initial, de reste � faire sont
			// �gaux � ceux de la tache
			else if (task.getSubTasksCount() == 0) {
				// Pr�paration de la requ�te
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
				// Param�tre pour la clause 'LIKE'
				String pathLike = task.getFullPath() + "%"; //$NON-NLS-1$

				// Pr�paration de la requ�te
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

			// Ex�cution de la requ�te
			log.debug("Request : " + baseRequest); //$NON-NLS-1$
			rs = pStmt.executeQuery();

			// Extraction du r�sultat
			Contribution[] result = rsToContributions(rs);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Calcule le total des contributions associ�e aux param�tres sp�cifi�s.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param contributor
	 *            le collaborateur associ� aux contributions (facultatif).
	 * @param fromDate
	 *            la date de d�part.
	 * @param toDate
	 *            la date de fin.
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static long getContributionsSum(DbTransaction tx, Task task,
			Collaborator contributor, Calendar fromDate, Calendar toDate)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			StringBuffer request = new StringBuffer(
					"select sum(ctb_duration) from contribution");
			if (task != null)
				request.append(", task");
			if (contributor != null)
				request.append(", collaborator");
			request.append(" where ");
			if (task != null) {
				request.append("ctb_task=tsk_id and tsk_id=?");
			}
			if (contributor != null) {
				if (task != null)
					request.append(" and ");
				request.append("ctb_contributor=clb_id and clb_id=?");
			}
			// Y'a-t-il un crit�re de date � ajouter ?
			if (fromDate != null || toDate != null) {
				request.append(" and ( ctb_year*10000 + ( ctb_month*100 + ctb_day ) )");
				// Deux dates sp�cifi�es
				if (fromDate != null && toDate != null)
					request.append(" between ? and ?");
				// Seule la date de d�but sp�cifi�e
				else if (fromDate != null)
					request.append(">=?");
				// Seule la date de fin sp�cifi�e
				else
					request.append("<=?");
			}

			// Calcul du consomm�
			log.debug("request : " + request);
			pStmt = tx.prepareStatement(request.toString()); //$NON-NLS-1$
			int idx = 1;
			if (task != null)
				pStmt.setLong(idx++, task.getId());
			if (contributor != null)
				pStmt.setLong(idx++, contributor.getId());

			// Y'a-t-il un crit�re de date � ajouter ?
			if (fromDate != null || toDate != null) {
				int i = 2;
				if (fromDate != null)
					pStmt.setString(i++, sdf.format(fromDate.getTime()));
				if (toDate != null)
					pStmt.setString(i++, sdf.format(toDate.getTime()));
			}
			// Ex�cution de le requ�te et extraction du r�sultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			long contributionSums = rs.getLong(1);
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return contributionSums;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					"Erreur lors du calcul du consomm� d'un collaborateur sur un intervalle de temps donn�",
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
	 * Calcule le nombre des contributions associ�e aux param�tres sp�cifi�s.
	 * 
	 * <p>
	 * Tous les param�tres sont facultatifs. Chaque param�tre sp�cifi� ag�t
	 * comme un filtre sur le r�sultat. A l'inverse, l'omission d'un param�tre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut consid�r�.
	 * </p>
	 * 
	 * <p>
	 * En sp�cifiant la tache X, on conna�tra la somme des contribution pour la
	 * taches X. En ne sp�cifiant pas de tache, la somme sera effectu�e quelque
	 * soit les t�ches.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la t�che associ�e aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associ� aux contributions (facultatif).
	 * @param year
	 *            l'ann�e (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static long getContributionsNb(DbTransaction tx, Task task,
			Collaborator contributor, Integer year, Integer month, Integer day)
			throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		return getContributionsAggregation(tx,
				"count(ctb_duration)", task, contributor, year, month, day); //$NON-NLS-1$
	}

	/**
	 * Calcule le cumuls des consommations associees aux contributions pour les
	 * param�tres sp�cifi�s.
	 * 
	 * <p>
	 * Tous les param�tres sont facultatifs. Chaque param�tre sp�cifi� ag�t
	 * comme un filtre sur le r�sultat. A l'inverse, l'omission d'un param�tre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut consid�r�.
	 * </p>
	 * 
	 * <p>
	 * En sp�cifiant la tache X, on conna�tra la somme des contribution pour la
	 * taches X. En ne sp�cifiant pas de tache, la somme sera effectu�e quelque
	 * soit les t�ches.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la t�che associ�e aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associ� aux contributions (facultatif).
	 * @param year
	 *            l'ann�e (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static long getContributionsSum(DbTransaction tx, Task task,
			Collaborator contributor, Integer year, Integer month, Integer day)
			throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		return getContributionsAggregation(tx,
				"sum(ctb_duration)", task, contributor, year, month, day); //$NON-NLS-1$
	}

	/**
	 * Calcule une aggregation associee aux contributions pour les param�tres
	 * sp�cifi�s.
	 * 
	 * <p>
	 * Tous les param�tres sont facultatifs. Chaque param�tre sp�cifi� ag�t
	 * comme un filtre sur le r�sultat. A l'inverse, l'omission d'un param�tre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut consid�r�.
	 * </p>
	 * 
	 * <p>
	 * En sp�cifiant la tache X, on conna�tra la somme des contribution pour la
	 * taches X. En ne sp�cifiant pas de tache, la somme sera effectu�e quelque
	 * soit les t�ches.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param aggregation
	 *            la cha�ne repr�sentant l'aggr�gation (ex:
	 *            <code>sum(ctb_contribution)</code>).
	 * @param task
	 *            la t�che associ�e aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associ� aux contributions (facultatif).
	 * @param year
	 *            l'ann�e (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	private static long getContributionsAggregation(DbTransaction tx,
			String aggregation, Task task, Collaborator contributor,
			Integer year, Integer month, Integer day) throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			StringBuffer baseRequest = new StringBuffer("select ") //$NON-NLS-1$
					.append(aggregation).append(
							" from contribution, task where ctb_task=tsk_id"); //$NON-NLS-1$
			// Cas ou la tache n'est pas sp�cifi�e
			if (task == null) {
				// Pr�paration de la requ�te
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				completeContributionReqParams(pStmt, 1, contributor, year,
						month, day);
			}
			// Si la tache n'admet pas de sous-taches, le cumul de
			// budget, de consomm� initial, de reste � faire sont
			// �gaux � ceux de la tache
			else if (task.getSubTasksCount() == 0) {
				// Pr�paration de la requ�te
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
				// Param�tre pour la clause 'LIKE'
				String pathLike = task.getFullPath() + "%"; //$NON-NLS-1$

				// Pr�paration de la requ�te
				baseRequest.append(" and tsk_path like ?"); //$NON-NLS-1$
				completeContributionRequest(baseRequest, contributor, year,
						month, day);
				String request = baseRequest.toString();
				pStmt = tx.prepareStatement(request);
				pStmt.setString(1, pathLike);
				completeContributionReqParams(pStmt, 2, contributor, year,
						month, day);
			}

			// Ex�cution de la requ�te
			log.debug("Request : " + baseRequest); //$NON-NLS-1$
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			long agregation = rs.getLong(1);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            l'identifiant de la dur�e.
	 * @return la dur�e.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Duration getDuration(DbTransaction tx, long durationId)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select dur_id, dur_is_active from duration where dur_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, durationId);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Re�cup�ration du r�sultat
			Duration duration = null;
			if (rs.next())
				duration = rsToDuration(rs);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            bool�en indiquant si l'on ne doit retourner que les
	 *            collaborateurs actifs.
	 * @return la liste des dur�es.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Duration[] getDurations(DbTransaction tx,
			boolean onlyActiveCollaborators) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			StringBuffer request = new StringBuffer(
					"select dur_id, dur_is_active from duration "); //$NON-NLS-1$
			if (onlyActiveCollaborators)
				request.append("where dur_is_active=?"); //$NON-NLS-1$
			request.append("order by dur_id asc"); //$NON-NLS-1$
			pStmt = tx.prepareStatement(request.toString());
			if (onlyActiveCollaborators)
				pStmt.setBoolean(1, true);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Duration> list = new ArrayList<Duration>();
			while (rs.next())
				list.add(rsToDuration(rs));

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Convertit le r�sultat d'une requ�te en dur�e.
	 * 
	 * @param rs
	 *            le result set.
	 * @return la dur�e.
	 * @throws SQLException
	 *             lev� en cas de probl�me SQL.
	 */
	private static Duration rsToDuration(ResultSet rs) throws SQLException {
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
	 * @return la tache parent d'une tache sp�cifi�e.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task getParentTask(DbTransaction tx, Task task)
			throws DbException {
		Task parentTask = null;
		String parentTaskFullPath = task.getPath();
		// Si le chemin est vide, la tache parent est nulle (tache racine)
		if (parentTaskFullPath != null && !"".equals(parentTaskFullPath)) { //$NON-NLS-1$
			// Extraction du chemin et du num�ro de la tache recherch�e
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
		// Retour du r�sultat
		return parentTask;
	}

	/**
	 * @param tx
	 *            le contexte de transaction.
	 * @param path
	 *            le chemin dont on veut conna�tre les taches.
	 * @return la liste des taches associ�es � un chemin donn�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task[] getTasks(DbTransaction tx, String path)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select tsk_id from task where tsk_path=? order by tsk_number"); //$NON-NLS-1$
			pStmt.setString(1, path);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Task> list = new ArrayList<Task>();
			while (rs.next()) {
				long taskId = rs.getLong(1);
				Task task = getTask(tx, taskId);
				list.add(task);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			log.debug("  => found " + list.size() + " entrie(s)"); //$NON-NLS-1$ //$NON-NLS-2$
			return (Task[]) list.toArray(new Task[list.size()]);
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
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task[] getSubtasks(DbTransaction tx, Task parentTask)
			throws DbException {
		// R�cup�ration du chemin � partir de la tache parent
		String fullpath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
		log.debug("Looking for tasks with path='" + fullpath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return getTasks(tx, fullpath);
	}

	/**
	 * Retourn la liste des taches correspondant au filtre de recherche
	 * sp�cifi�.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param filter
	 *            le filtre de recherche.
	 * @return la liste des taches correspondant au filtre de recherche
	 *         sp�cifi�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task[] getTasks(DbTransaction tx, TaskSearchFilter filter)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			StringBuffer request = new StringBuffer(
					"select tsk_id from task where "); //$NON-NLS-1$
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
			// Ajout du crit�re de comparaison
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
			// Pr�paration de la requ�te
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

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// R�cup�ration du r�sultat
			ArrayList<Task> list = new ArrayList<Task>();
			while (rs.next()) {
				long taskId = rs.getLong(1);
				Task task = getTask(tx, taskId);
				list.add(task);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Pr�paration du r�sultat
			Task[] tasks = (Task[]) list.toArray(new Task[list.size()]);

			// On trie les taches manuellement car le tri base de donn�es
			// pose un probl�me dans la mesure ou la BDD consid�re le champ
			// tsk_path comme num�rique pour le tri ce qui pose un pb
			// Ex :
			// ROOT (path : 01)
			// +- T1 (path : 0101)
			// | +- T11 (path : 010101)
			// | +- T12 (path : 010102)
			// +- T2 (path : 0102)
			// Si on ram�ne l'ensemble des sous taches de ROOT, on voudrait
			// avoir
			// dans l'ordre T1, T11, T12, T2
			// Avec un tri base de donn�e, on obtiendrait T1, T2, T11, T12 ; T2
			// ne se
			// trouve pas ou on l'attend, ceci en raison du fait qu'en
			// comparaison
			// num�rique 0102 est < � 010101 et � 010102. Par contre, en
			// comparaison
			// de cha�nes (en java), on a bien 0102 > 010101 et 010102.
			Arrays.sort(tasks, new Comparator<Task>() {
				public int compare(Task t1, Task t2) {
					return t1.getFullPath().compareTo(t2.getFullPath());
				}

			});

			// Retour du r�sultat
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
	 * @param taskId
	 *            l'identifiant de la tache recherch�e.
	 * @return la tache dont l'identifiant est sp�cifi�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task getTask(DbTransaction tx, long taskId)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select tsk_path, tsk_number, tsk_code, tsk_name, tsk_budget, tsk_initial_cons, tsk_todo, tsk_comment from task where tsk_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, taskId);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Pr�paration du r�sultat
			Task task = null;
			if (rs.next()) {
				task = new Task();
				task.setId(taskId);
				task.setPath(rs.getString(1));
				task.setNumber(rs.getByte(2));
				task.setCode(rs.getString(3));
				task.setName(rs.getString(4));
				task.setBudget(rs.getLong(5));
				task.setInitiallyConsumed(rs.getLong(6));
				task.setTodo(rs.getLong(7));
				task.setComment(rs.getString(8));
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Si la tache existe bien
			if (task != null) {
				// Recherche du nombre de sous-taches
				String taskFullPath = task.getFullPath();
				pStmt = tx
						.prepareStatement("select count(*) from task where tsk_path=?"); //$NON-NLS-1$
				pStmt.setString(1, taskFullPath);

				// Ex�cution de la requ�te
				rs = pStmt.executeQuery();
				if (rs.next()) {
					int subTasksCount = rs.getInt(1);
					task.setSubTasksCount(subTasksCount);
				}
				// Fermeture du ResultSet
				pStmt.close();
				pStmt = null;
			}

			// Retour du r�sultat
			return task;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.TASK_SELECTION_BY_ID_FAILURE", new Long(taskId)), e); //$NON-NLS-1$ //$NON-NLS-2$
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
	 *            le chemin de la tache recherch�e.
	 * @param taskNumber
	 *            le num�ro de la tache recherch�e.
	 * @return la tache dont le chemin et le num�ro sont sp�cifi�s.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task getTask(DbTransaction tx, String taskPath,
			byte taskNumber) throws DbException {
		log.debug("getTask(" + taskPath + ", " + taskNumber + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select tsk_id from task where tsk_path=? and tsk_number=?"); //$NON-NLS-1$
			pStmt.setString(1, taskPath);
			pStmt.setByte(2, taskNumber);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Pr�paration du r�sultat
			Task task = null;
			if (rs.next()) {
				long taskId = rs.getLong(1);
				task = getTask(tx, taskId);
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            le chemin de la tache recherch�e.
	 * @param taskCode
	 *            le code de la tache recherch�e.
	 * @return la tache dont le code et la tache parent sont sp�cifi�s.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task getTask(DbTransaction tx, String taskPath,
			String taskCode) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select tsk_id from task where tsk_path=? and tsk_code=?"); //$NON-NLS-1$
			pStmt.setString(1, taskPath);
			pStmt.setString(2, taskCode);

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Pr�paration du r�sultat
			Task task = null;
			if (rs.next()) {
				long taskId = rs.getLong(1);
				task = getTask(tx, taskId);
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            date de d�but.
	 * @param toDate
	 *            date de fin.
	 * @return la liste de taches associ�es au collaborateur entre les 2 dates
	 *         sp�cifi�es.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task[] getTasks(DbTransaction tx,
			Collaborator collaborator, Calendar fromDate, Calendar toDate)
			throws DbException {
		log.debug("getTasks(" + collaborator + ", " + sdf.format(fromDate.getTime()) + ", " + sdf.format(toDate.getTime()) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("select distinct ctb_task, tsk_path, tsk_number from contribution, task where ctb_task=tsk_id and ctb_contributor=? and ctb_year*10000 + ( ctb_month*100 + ctb_day ) between ? and ? order by tsk_path, tsk_number"); //$NON-NLS-1$
			pStmt.setLong(1, collaborator.getId());
			pStmt.setString(2, sdf.format(fromDate.getTime()));
			pStmt.setString(3, sdf.format(toDate.getTime()));

			// Ex�cution de la requ�te
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Task> list = new ArrayList<Task>();
			while (rs.next()) {
				long taskId = rs.getLong(1);
				Task task = getTask(tx, taskId);
				list.add(task);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			log.debug("  => found " + list.size() + " entrie(s)"); //$NON-NLS-1$ //$NON-NLS-2$
			return (Task[]) list.toArray(new Task[list.size()]);
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
	 *            la t�che pour laquelle on souhaite conna�tre les totaux.
	 * @param fromDate
	 *            date de d�part � prendre en compte pour le calcul.
	 * @param toDate
	 *            date de fin � prendre en compte pour le calcul.
	 * @return les totaux associ�s � une tache (consomm�, etc.).
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static TaskSums getTaskSums(DbTransaction tx, Task task,
			Calendar fromDate, Calendar toDate) throws DbException {
		// TODO Factoriser cette m�those avec getContributionsSum
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Pr�paration du r�sultat
			TaskSums taskSums = new TaskSums();
			boolean taskIsLeaf = task != null && task.getSubTasksCount() == 0;

			/**
			 * Calcul de la partie ind�pendante des contributions (budget /
			 * consommation initiale / reste � faire
			 */

			// Si la tache n'admet pas de sous-taches, le cumul de
			// budget, de consomm� initial, de reste � faire sont
			// �gaux � ceux de la tache
			if (taskIsLeaf) {
				taskSums.setBudgetSum(task.getBudget());
				taskSums.setInitiallyConsumedSum(task.getInitiallyConsumed());
				taskSums.setTodoSum(task.getTodo());
			}
			// Sinon, il faut calculer
			else {
				// Calcul des cumuls
				pStmt = tx
						.prepareStatement("select sum(tsk_budget), sum(tsk_initial_cons), sum(tsk_todo) from task where tsk_path like ?"); //$NON-NLS-1$
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
			 * Calcul du consomm�
			 */

			// Pr�paration de la requ�te
			StringBuffer request = new StringBuffer(
					"select sum(ctb_duration), count(ctb_duration) from contribution, task where ctb_task=tsk_id and ");
			// En fonction du cas, on recherche soit sur la tache pr�cise
			// (tsk_id=?), soit sur un arbre (tsk_path like ?)
			request.append(taskIsLeaf ? "tsk_id=?" : "tsk_path like ?");
			// Y'a-t-il un crit�re de date � ajouter ?
			if (fromDate != null || toDate != null) {
				request.append(" and ( ctb_year*10000 + ( ctb_month*100 + ctb_day ) )");
				// Deux dates sp�cifi�es
				if (fromDate != null && toDate != null)
					request.append(" between ? and ?");
				// Seule la date de d�but sp�cifi�e
				else if (fromDate != null)
					request.append(">=?");
				// Seule la date de fin sp�cifi�e
				else
					request.append("<=?");
			}

			// Calcul du consomm�
			pStmt = tx.prepareStatement(request.toString()); //$NON-NLS-1$
			// En fonction du cas, on recherche soit sur la tache pr�cise
			// (tsk_id=?), soit sur un arbre (tsk_path like ?)
			if (taskIsLeaf)
				pStmt.setLong(1, task.getId());
			else
				pStmt.setString(1, (task == null ? "" : task.getFullPath())
						+ "%");
			// Y'a-t-il un crit�re de date � ajouter ?
			if (fromDate != null || toDate != null) {
				int i = 2;
				if (fromDate != null)
					pStmt.setString(i++, sdf.format(fromDate.getTime()));
				if (toDate != null)
					pStmt.setString(i++, sdf.format(toDate.getTime()));
			}
			// Ex�cution de le requ�te et extraction du r�sultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			taskSums.setConsumedSum(rs.getLong(1));
			taskSums.setContributionsNb(rs.getLong(2));
			pStmt.close();
			pStmt = null;

			/**
			 * Si un crit�re de date de fin a �t� sp�cifi�, il faut corriger le
			 * RAF calcul� plus haut sinon on ne tient pas compte des
			 * consommations au dela de cette date. En effet RAF � une date
			 * donn�e = RAF identifi� au niveau de la tache + consommations
			 * futures d�j� enregistr�es dans le syst�me
			 */
			if (toDate != null) {
				request = new StringBuffer(
						"select sum(ctb_duration) from contribution, task where ctb_task=tsk_id and ");
				request.append(taskIsLeaf ? "tsk_id=?" : "tsk_path like ?");
				request.append(" and ( ctb_year*10000 + ( ctb_month*100 + ctb_day ) ) > ?");
				// Calcul des consommations au del� de la date de fin sp�cifi�e
				pStmt = tx.prepareStatement(request.toString()); //$NON-NLS-1$
				if (taskIsLeaf)
					pStmt.setLong(1, task.getId());
				else
					pStmt.setString(1, (task == null ? "" : task.getFullPath())
							+ "%");
				pStmt.setString(2, sdf.format(toDate.getTime()));
				// Ex�cution de le requ�te et extraction du r�sultat
				rs = pStmt.executeQuery();
				if (!rs.next())
					throw new DbException(
							Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
				// Mise � jour du RAF
				taskSums.setTodoSum(taskSums.getTodoSum() + rs.getLong(1));
				pStmt.close();
				pStmt = null;
			}

			/**
			 * Retour du r�sultat
			 */

			// Retour du r�sultat
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
	 *            le collaborateur � supprimer.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void removeCollaborator(DbTransaction tx,
			Collaborator collaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("delete from collaborator where clb_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, collaborator.getId());

			// Ex�cution de la requ�te
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
	 *            la contribution � supprimer.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void removeContribution(DbTransaction tx,
			Contribution contribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("delete from contribution where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
			pStmt.setInt(1, contribution.getYear());
			pStmt.setInt(2, contribution.getMonth());
			pStmt.setInt(3, contribution.getDay());
			pStmt.setLong(4, contribution.getContributorId());
			pStmt.setLong(5, contribution.getTaskId());

			// Ex�cution de la requ�te
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
	 * Supprime une dur�e du r�f�rentiel de dur�es.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la dur�e � supprimer.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void removeDuration(DbTransaction tx, Duration duration)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx.prepareStatement("delete from duration where dur_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, duration.getId());

			// Ex�cution de la requ�te
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
	 *            la tache � supprimer.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void removeTask(DbTransaction tx, Task task)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Control sur les sous taches
			Task[] subTasks = DbMgr.getSubtasks(tx, task);
			for (int i = 0; i < subTasks.length; i++) {
				DbMgr.removeTask(tx, subTasks[i]);
			}

			// Pr�paration de la requ�te
			pStmt = tx.prepareStatement("delete from task where tsk_id=?"); //$NON-NLS-1$
			pStmt.setLong(1, task.getId());

			// Ex�cution de la requ�te
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
	 * Annule le modifications effectu�es dans le cadre d'une transactrion.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static void rollbackTransaction(DbTransaction tx)
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
	 *            le collaborateur � modifier.
	 * @return le collaborateur modifi�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Collaborator updateCollaborator(DbTransaction tx,
			Collaborator collaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("update collaborator set clb_login=?, clb_first_name=?, clb_last_name=?, clb_is_active=? where clb_id=?"); //$NON-NLS-1$
			pStmt.setString(1, collaborator.getLogin());
			pStmt.setString(2, collaborator.getFirstName());
			pStmt.setString(3, collaborator.getLastName());
			pStmt.setBoolean(4, collaborator.getIsActive());
			pStmt.setLong(5, collaborator.getId());

			// Ex�cution de la requ�te
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            la contribution � modifier.
	 * @return la contribution modifi�e.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Contribution updateContribution(DbTransaction tx,
			Contribution contribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("update contribution set ctb_duration=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
			pStmt.setLong(1, contribution.getDurationId());
			pStmt.setInt(2, contribution.getYear());
			pStmt.setInt(3, contribution.getMonth());
			pStmt.setInt(4, contribution.getDay());
			pStmt.setLong(5, contribution.getContributorId());
			pStmt.setLong(6, contribution.getTaskId());

			// Ex�cution de la requ�te
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Met � jour une dur�e.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la dur�e � mettre � jour.
	 * @return la dur�e mise � jour.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Duration updateDuration(DbTransaction tx, Duration duration)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("update duration set dur_is_active=? where dur_id=?"); //$NON-NLS-1$
			pStmt.setBoolean(1, duration.getIsActive());
			pStmt.setLong(2, duration.getId());

			// Ex�cution de la requ�te
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            la tache � affecter.
	 * @return la contribution mise � jour.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Contribution changeContributionTask(DbTransaction tx,
			Contribution contribution, Task newContributionTask)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("update contribution set ctb_task=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
			pStmt.setLong(1, newContributionTask.getId());
			pStmt.setInt(2, contribution.getYear());
			pStmt.setInt(3, contribution.getMonth());
			pStmt.setInt(4, contribution.getDay());
			pStmt.setLong(5, contribution.getContributorId());
			pStmt.setLong(6, contribution.getTaskId());

			// Ex�cution de la requ�te
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Mise � jour de la contribution
			contribution.setTaskId(newContributionTask.getId());

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            la tache � modifier.
	 * @return la tache modifi�e.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static Task updateTask(DbTransaction tx, Task task)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Pr�paration de la requ�te
			pStmt = tx
					.prepareStatement("update task set tsk_path=?, tsk_number=?, tsk_code=?, tsk_name=?, tsk_budget=?, tsk_initial_cons=?, tsk_todo=?, tsk_comment=? where tsk_id=?"); //$NON-NLS-1$
			pStmt.setString(1, task.getPath());
			pStmt.setByte(2, task.getNumber());
			pStmt.setString(3, task.getCode());
			pStmt.setString(4, task.getName());
			pStmt.setLong(5, task.getBudget());
			pStmt.setLong(6, task.getInitiallyConsumed());
			pStmt.setLong(7, task.getTodo());
			pStmt.setString(8, task.getComment());
			pStmt.setLong(9, task.getId());

			// Ex�cution de la requ�te
			int updated = pStmt.executeUpdate();
			if (updated != 1)
				throw new SQLException(
						Strings.getString("DbMgr.errors.SQL_UPDATE_FAILURE")); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 *            buffer utilis� pour la construction de la requ�te.
	 * @param contributor
	 *            le collaborateur ayant contribu� � la tache (facultatif).
	 * @param year
	 *            l'ann�e (facultative)
	 * @param month
	 *            le mois (facultatif)
	 * @param day
	 *            le jour (facultatif)
	 */
	private static void completeContributionRequest(StringBuffer requestBase,
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
	 * Complete les param�tres de la requete de calcul de la somme des
	 * contributions.
	 * 
	 * @param pStmt
	 *            le statement.
	 * @param startIndex
	 * @param contributor
	 *            le collaborateur ayant contribu� � la tache (facultatif).
	 * @param year
	 *            l'ann�e (facultative)
	 * @param month
	 *            le mois (facultatif)
	 * @param day
	 *            le jour (facultatif)
	 * @throws SQLException
	 *             lev� en cas d'incident avec la base de donn�es.
	 */
	private static void completeContributionReqParams(PreparedStatement pStmt,
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
	 * G�n�re un nouveau num�ro de tache pour un chemin donn�.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param path
	 *            le chemin consid�r�.
	 * @return le num�ro g�n�r�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	protected static byte newTaskNumber(DbTransaction tx, String path)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Recherche du max
			pStmt = tx
					.prepareStatement("select max(tsk_number) from task where tsk_path=?"); //$NON-NLS-1$
			pStmt.setString(1, path);
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			byte max = rs.getByte(1);
			log.debug("  => max= : " + max); //$NON-NLS-1$

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
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
	 * Retourne l'identifiant g�n�r� automatiquement par la base de donn�es.
	 * 
	 * @param pStmt
	 *            le statement SQL.
	 * @return l'identifiant g�n�r�.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	private static long getGeneratedId(PreparedStatement pStmt)
			throws DbException {
		long generatedId = -1;
		PreparedStatement pStmt1 = null;
		try {
			// R�cup�ration de la connexion
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
				// R�cup�ration de l'identifiant g�n�r�
				ResultSet rs = pStmt.getGeneratedKeys();
				if (!rs.next())
					throw new DbException(
							Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
				generatedId = rs.getLong(1);
			}
			// Retour du r�sultat
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
	 * Indique si la BDD de donn�es est une base HSQLDB.
	 * 
	 * @param con
	 *            la connexion SQL.
	 * @return un bool�en indiquant si la BDD est de type HSQLDB.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	private static boolean isHSQLDB(Connection con) throws DbException {
		try {
			// R�cup�ration du nom de la base de donn�es
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
	 * Indique si la BDD de donn�es est une base HSQLDB embarqu�e.
	 * 
	 * @param con
	 *            la connexion SQL.
	 * @return un bool�en indiquant si la BDD est de type HSQLDB embarqu�e.
	 * @throws DbException
	 *             lev� en cas d'incident technique d'acc�s � la base.
	 */
	private static boolean isEmbeddedHSQLDB(Connection con) throws DbException {
		try {
			// R�cup�ration du nom de la base de donn�es
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
