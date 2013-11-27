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
package org.activitymgr.core.impl;

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

import org.activitymgr.core.DbException;
import org.activitymgr.core.DbTransaction;
import org.activitymgr.core.IDbMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Classe offrant les services de base de persistence de l'application.
 */
public class DbMgrImpl implements IDbMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(DbMgrImpl.class);

	/** Formatteur de date */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

	/** Transaction provider */
	private Provider<DbTransaction> tx;

	/**
	 * Default constructor.
	 * 
	 * @param tx
	 *            transaction provider.
	 */
	@Inject
	public DbMgrImpl(Provider<DbTransaction> tx) {
		this.tx = tx;
	}

	/**
	 * @return the current transaction.
	 */
	private DbTransaction tx() {
		return tx.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#tablesExist()
	 */
	@Override
	public boolean tablesExist() throws DbException {
		boolean tablesExist = true;
		tablesExist &= tableExists("COLLABORATOR"); //$NON-NLS-1$
		tablesExist &= tableExists("CONTRIBUTION"); //$NON-NLS-1$
		tablesExist &= tableExists("DURATION"); //$NON-NLS-1$
		tablesExist &= tableExists("TASK"); //$NON-NLS-1$
		return tablesExist;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#tableExists(java.lang.String)
	 */
	@Override
	public boolean tableExists(String tableName) throws DbException {
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx().getConnection();

			// Recherche de la table
			rs = con.getMetaData().getTables(null, null, tableName,
					new String[] { "TABLE" }); //$NON-NLS-1$

			// Récupération du résultat
			boolean exists = rs.next();
			rs.close();

			// Retour du résultat
			return exists;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.SQL_TABLES_DETECTION_FAILURE", tableName), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			lastAttemptClose(rs);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#createTables()
	 */
	@Override
	public void createTables() throws DbException {
		// Lecture du fichier SQL de création de la BDD
		String batchName = (tx().isHsqlOrH2() ? "hsqldb.sql" : "mysqldb.sql"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		InputStream in = DbMgrImpl.class.getResourceAsStream(batchName);
		executeScript(in);

		// Test de l'existence des tables
		if (!tablesExist())
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_TABLE_CREATION_FAILURE"), null); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#executeScript(java.io.InputStream)
	 */
	@Override
	public void executeScript(InputStream scriptContent) throws DbException {
		try {
			// Script content retrieval
			String sql = StringHelper.fromInputStream(scriptContent);

			// Execute the script
			executeScript(sql);
		} catch (IOException e) {
			log.info("I/O error while loading table creation SQL script.", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_SCRIPT_LOAD_FAILURE"), null); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#executeScript(java.lang.String)
	 */
	@Override
	public void executeScript(String scriptContent) throws DbException {
		Statement stmt = null;
		try {
			// Découpage et exécution du batch
			stmt = tx().createStatement();
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
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException("Database table creation failure", e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(stmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#createCollaborator(org.activitymgr.core.beans
	 * .Collaborator)
	 */
	@Override
	public Collaborator createCollaborator(Collaborator newCollaborator)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"insert into COLLABORATOR (clb_login, clb_first_name, clb_last_name, clb_is_active) values (?, ?, ?, ?)", true); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#createContribution(org.activitymgr.core.beans
	 * .Contribution)
	 */
	@Override
	public Contribution createContribution(Contribution newContribution)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"insert into CONTRIBUTION (ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration) values (?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#createDuration(org.activitymgr.core.beans
	 * .Duration)
	 */
	@Override
	public Duration createDuration(Duration newDuration) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"insert into DURATION (dur_id, dur_is_active) values (?, ?)"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#createTask(org.activitymgr.core.beans.Task,
	 * org.activitymgr.core.beans.Task)
	 */
	@Override
	public Task createTask(Task parentTask, Task newTask) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Mise à jour du chemin de la tâche
			String parentPath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
			newTask.setPath(parentPath);

			// Génération du numéro de la tâche
			byte taskNumber = newTaskNumber(parentPath);
			newTask.setNumber(taskNumber);

			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"insert into TASK (tsk_path, tsk_number, tsk_code, tsk_name, tsk_budget, tsk_initial_cons, tsk_todo, tsk_comment) values (?, ?, ?, ?, ?, ?, ?, ?)", true); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#durationIsUsed(org.activitymgr.core.beans
	 * .Duration)
	 */
	@Override
	public boolean durationIsUsed(Duration duration) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx().prepareStatement(
					"select count(*) from CONTRIBUTION where ctb_duration=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getCollaborator(long)
	 */
	@Override
	public Collaborator getCollaborator(long collaboratorId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			pStmt = tx()
					.prepareStatement(
							"select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from COLLABORATOR where clb_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
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
	private Collaborator rsToCollaborator(ResultSet rs) throws SQLException {
		Collaborator collaborator = new Collaborator();
		collaborator.setId(rs.getLong(1));
		collaborator.setLogin(rs.getString(2));
		collaborator.setFirstName(rs.getString(3));
		collaborator.setLastName(rs.getString(4));
		collaborator.setIsActive(rs.getBoolean(5));
		return collaborator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getCollaborator(java.lang.String)
	 */
	@Override
	public Collaborator getCollaborator(String login) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"select clb_id, clb_login, clb_first_name, clb_last_name, clb_is_active from COLLABORATOR where clb_login=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getCollaborators(int, boolean, boolean)
	 */
	@Override
	public Collaborator[] getCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort, boolean onlyActiveCollaborators)
			throws DbException {
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
			pStmt = tx().prepareStatement(request.toString());
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
			lastAttemptToClose(pStmt);
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
	private Contribution[] rsToContributions(ResultSet rs) throws SQLException {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getContributions(org.activitymgr.core.beans
	 * .Collaborator, org.activitymgr.core.beans.Task, java.util.Calendar,
	 * java.util.Calendar)
	 */
	@Override
	public Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate,
					"ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration");

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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getContributionsSum(org.activitymgr.core.
	 * beans.Collaborator, org.activitymgr.core.beans.Task, java.util.Calendar,
	 * java.util.Calendar)
	 */
	@Override
	public long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the SQL request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate, "sum(ctb_duration)");

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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getContributionsCount(org.activitymgr.core
	 * .beans.Collaborator, org.activitymgr.core.beans.Task, java.util.Calendar,
	 * java.util.Calendar)
	 */
	@Override
	public int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the SQL request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate, "count(ctb_duration)");

			// Exécution de le requête et extraction du résultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			int count = rs.getInt(1);
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return count;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					"Erreur lors du calcul du consommé d'un collaborateur sur un intervalle de temps donné",
					e);
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/**
	 * Builds a request that selects contributions using a given task,
	 * contributor and date interval.
	 * 
	 * <p>
	 * All parameters are optionnal.
	 * </p>
	 * 
	 * @param task
	 *            a parent task of the contributions tasks.
	 * @param contributor
	 *            the contributor.
	 * @param fromDate
	 *            start date of the interval.
	 * @param toDate
	 *            end date of the interval.
	 * @param fieldsToSelect
	 *            fields to select.
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement buildContributionsRequest(Task task,
			Collaborator contributor, Calendar fromDate, Calendar toDate,
			String fieldsToSelect) throws SQLException {
		// Préparation de la requête
		StringBuffer request = new StringBuffer("select ")
				.append(fieldsToSelect);
		request.append(" from CONTRIBUTION");
		if (task != null) {
			request.append(", TASK");
		}
		return buildIntervalRequest(request, contributor, task, fromDate,
				toDate, true, null);
	}

	/**
	 * Builds a interval request (a request that handles a date interval).
	 * 
	 * @param request
	 *            the request buffer.
	 * @param contributor
	 *            the contributor to consider (optionnal).
	 * @param task
	 *            the task to consider (optionnal).
	 * @param fromDate
	 *            the start date of the interval to consider (optionnal).
	 * @param toDate
	 *            the end date of the interval to consider (optionnal).
	 * @param insertWhereClause
	 *            <code>true</code> if a <code>where</code> keyword must be
	 *            inserted.
	 * @param orderByClause
	 *            the order by clause.
	 * @return the request.
	 * @throws SQLException
	 *             thrown if a SQL exception occurs.
	 */
	private PreparedStatement buildIntervalRequest(StringBuffer request,
			Collaborator contributor, Task task, Calendar fromDate,
			Calendar toDate, boolean insertWhereClause, String orderByClause)
			throws SQLException {
		PreparedStatement pStmt;
		if (contributor != null) {
			request.append(insertWhereClause ? " where" : " and");
			insertWhereClause = false;
			request.append(" ctb_contributor=?");
		}
		if (task != null) {
			request.append(insertWhereClause ? " where" : " and");
			insertWhereClause = false;
			request.append(" ctb_task=tsk_id and (tsk_id=? or tsk_path like ?)");
		}
		// Conversion des dates
		String fromDateStr = fromDate != null ? sdf.format(fromDate.getTime())
				: null;
		String toDateStr = toDate != null ? sdf.format(toDate.getTime()) : null;
		if (fromDate != null || toDate != null) {
			request.append(insertWhereClause ? " where" : " and");
			insertWhereClause = false;
			request.append(" (ctb_year*10000 + ( ctb_month*100 + ctb_day ))");
			// If both dates are specified
			if (fromDate != null && toDate != null) {
				if (!fromDateStr.equals(toDateStr)) {
					request.append(" between ? and ?");
				} else {
					request.append(" = ?");
				}
			}
			// Else if only 'from' specified (toDate == null)
			else if (fromDate != null) {
				request.append(" >= ?");
			}
			// Else if only 'to' specified (fromDate == null)
			else {
				request.append(" <= ?");
			}
		}
		// Order by ?
		if (orderByClause != null) {
			request.append(" order by ");
			request.append(orderByClause);
		}
		// Execute request
		log.debug("request : " + request);
		pStmt = tx().prepareStatement(request.toString()); //$NON-NLS-1$
		int paramIdx = 1;
		if (contributor != null) {
			pStmt.setLong(paramIdx++, contributor.getId());
		}
		if (task != null) {
			pStmt.setLong(paramIdx++, task.getId());
			pStmt.setString(paramIdx++, task.getFullPath() + '%');
		}
		if (fromDate != null || toDate != null) {
			// If both dates are specified
			if (fromDate != null && toDate != null) {
				if (!fromDateStr.equals(toDateStr)) {
					pStmt.setString(paramIdx++, fromDateStr);
					pStmt.setString(paramIdx++, toDateStr);
				} else {
					pStmt.setString(paramIdx++, fromDateStr);
				}
			}
			// Else if only 'from' specified (toDate == null)
			else if (fromDate != null) {
				pStmt.setString(paramIdx++, fromDateStr);
			}
			// Else if only 'to' specified (fromDate == null)
			else {
				pStmt.setString(paramIdx++, toDateStr);
			}
		}
		return pStmt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getDuration(long)
	 */
	@Override
	public Duration getDuration(long durationId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"select dur_id, dur_is_active from DURATION where dur_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getDurations(boolean)
	 */
	@Override
	public Duration[] getDurations(boolean onlyActiveCollaborators)
			throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer(
					"select dur_id, dur_is_active from DURATION "); //$NON-NLS-1$
			if (onlyActiveCollaborators)
				request.append("where dur_is_active=? "); //$NON-NLS-1$
			request.append("order by dur_id asc"); //$NON-NLS-1$
			pStmt = tx().prepareStatement(request.toString());
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
			lastAttemptToClose(pStmt);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getParentTask(org.activitymgr.core.beans.
	 * Task)
	 */
	@Override
	public Task getParentTask(Task task) throws DbException {
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
			parentTask = getTask(path, number);
		}
		// Retour du résultat
		return parentTask;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTasks(java.lang.String)
	 */
	@Override
	public Task[] getTasks(String path) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"select tsk_id, tsk_number from TASK where tsk_path=? order by tsk_number"); //$NON-NLS-1$
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
			Task[] tasks = getTasks(taskIds);
			return tasks;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_SELECTION_BY_PATH", path), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getSubtasks(org.activitymgr.core.beans.Task)
	 */
	@Override
	public Task[] getSubtasks(Task parentTask) throws DbException {
		// Récupération du chemin à partir de la tache parent
		String fullpath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
		log.debug("Looking for tasks with path='" + fullpath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return getTasks(fullpath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTasks(org.activitymgr.core.beans.
	 * TaskSearchFilter)
	 */
	@Override
	public Task[] getTasks(TaskSearchFilter filter) throws DbException {
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
			pStmt = tx().prepareStatement(request.toString());
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
			Task[] tasks = getTasks(taskIds);

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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTask(long)
	 */
	@Override
	public Task getTask(long taskId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Request preparation
			StringWriter request = prepareSelectTaskRequest();
			request.append(" theTask.tsk_id=?");
			completeSelectTaskRequest(request);
			pStmt = tx().prepareStatement(request.toString());
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTasks(long[])
	 */
	@Override
	public Task[] getTasks(long[] tasksIds) throws DbException {
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
					pStmt = tx().prepareStatement(request.toString());
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
			lastAttemptToClose(pStmt);
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
		// HSQLDB expects all selected columns to be present in the group by
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
	private Task toTask(ResultSet rs) throws SQLException {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTask(java.lang.String, byte)
	 */
	@Override
	public Task getTask(String taskPath, byte taskNumber) throws DbException {
		log.debug("getTask(" + taskPath + ", " + taskNumber + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringWriter request = prepareSelectTaskRequest();
			request.append(" theTask.tsk_path=? and theTask.tsk_number=?");
			completeSelectTaskRequest(request);
			pStmt = tx().prepareStatement(request.toString());
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTask(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Task getTask(String taskPath, String taskCode) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringWriter request = prepareSelectTaskRequest();
			request.append(" theTask.tsk_path=? and theTask.tsk_code=?"); //$NON-NLS-1$
			completeSelectTaskRequest(request);
			pStmt = tx().prepareStatement(request.toString());
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
			lastAttemptToClose(pStmt);
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.IDbMgr#getContributors(org.activitymgr.core.beans.Task, java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public Collaborator[] getContributors(Task task, Calendar fromDate,
			Calendar toDate) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer();
			request.append("select distinct (ctb_contributor), clb_login, clb_first_name, clb_last_name, clb_is_active from CONTRIBUTION, COLLABORATOR");
			if (task != null) {
				request.append(", TASK");
			}
			request.append("  where ctb_contributor=clb_id");
			pStmt = buildIntervalRequest(request, null, task, fromDate,
					toDate, false, "clb_login");

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Collaborator> list = new ArrayList<Collaborator>();
			while (rs.next()) {
				list.add(rsToCollaborator(rs));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return (Collaborator[]) list.toArray(new Collaborator[list.size()]);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_COLLABORATOR_FAILURE"), e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getContributedTasks(org.activitymgr.core.beans.Collaborator
	 * , java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public Task[] getContributedTasks(Collaborator contributor, Calendar fromDate,
			Calendar toDate) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer();
			request.append("select distinct ctb_task, tsk_path, tsk_number from CONTRIBUTION, TASK where ctb_task=tsk_id");
			pStmt = buildIntervalRequest(request, contributor, null, fromDate,
					toDate, false, "tsk_path, tsk_number");

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
			return getTasks(taskIds);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_COLLABORATOR_FAILURE"), e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getTaskSums(org.activitymgr.core.beans.Task,
	 * java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public TaskSums getTaskSums(Task task, Calendar fromDate, Calendar toDate)
			throws DbException {
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
				pStmt = tx()
						.prepareStatement(
								"select sum(tsk_budget), sum(tsk_initial_cons), sum(tsk_todo) from TASK where tsk_path like ?"); //$NON-NLS-1$
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

			// Build the request
			pStmt = buildContributionsRequest(task, null, fromDate, toDate,
					"sum(ctb_duration), count(ctb_duration)");

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Extraction du résultat
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			taskSums.setConsumedSum(rs.getLong(1));
			taskSums.setContributionsNb(rs.getLong(2));

			// Fermeture du ResultSet
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
				// Build the request
				Calendar date = (Calendar) toDate.clone();
				date.add(Calendar.DATE, 1);
				pStmt = buildContributionsRequest(task, null, date, null,
						"sum(ctb_duration)");

				// Exécution de la requête
				rs = pStmt.executeQuery();

				// Extraction du résultat
				if (!rs.next())
					throw new DbException(
							Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
				taskSums.setTodoSum(taskSums.getTodoSum() + rs.getLong(1));

				// Fermeture du ResultSet
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#removeCollaborator(org.activitymgr.core.beans
	 * .Collaborator)
	 */
	@Override
	public void removeCollaborator(Collaborator collaborator)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx().prepareStatement(
					"delete from COLLABORATOR where clb_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#removeContribution(org.activitymgr.core.beans
	 * .Contribution)
	 */
	@Override
	public void removeContribution(Contribution contribution)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"delete from CONTRIBUTION where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#removeDuration(org.activitymgr.core.beans
	 * .Duration)
	 */
	@Override
	public void removeDuration(Duration duration) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement("delete from DURATION where dur_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#removeTask(org.activitymgr.core.beans.Task)
	 */
	@Override
	public void removeTask(Task task) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Control sur les sous taches
			Task[] subTasks = getSubtasks(task);
			for (int i = 0; i < subTasks.length; i++) {
				removeTask(subTasks[i]);
			}

			// Préparation de la requête
			pStmt = tx().prepareStatement("delete from TASK where tsk_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#updateCollaborator(org.activitymgr.core.beans
	 * .Collaborator)
	 */
	@Override
	public Collaborator updateCollaborator(Collaborator collaborator)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"update COLLABORATOR set clb_login=?, clb_first_name=?, clb_last_name=?, clb_is_active=? where clb_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#updateContribution(org.activitymgr.core.beans
	 * .Contribution)
	 */
	@Override
	public Contribution updateContribution(Contribution contribution)
			throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"update CONTRIBUTION set ctb_duration=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#updateDuration(org.activitymgr.core.beans
	 * .Duration)
	 */
	@Override
	public Duration updateDuration(Duration duration) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx().prepareStatement(
					"update DURATION set dur_is_active=? where dur_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#changeContributionTask(org.activitymgr.core
	 * .beans.Contribution, org.activitymgr.core.beans.Task)
	 */
	@Override
	public Contribution changeContributionTask(Contribution contribution,
			Task newContributionTask) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"update CONTRIBUTION set ctb_task=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#updateTask(org.activitymgr.core.beans.Task)
	 */
	@Override
	public Task updateTask(Task task) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Préparation de la requête
			pStmt = tx()
					.prepareStatement(
							"update TASK set tsk_path=?, tsk_number=?, tsk_code=?, tsk_name=?, tsk_budget=?, tsk_initial_cons=?, tsk_todo=?, tsk_comment=? where tsk_id=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#newTaskNumber(java.lang.String)
	 */
	@Override
	public byte newTaskNumber(String path) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Recherche du max
			pStmt = tx().prepareStatement(
					"select max(tsk_number) from TASK where tsk_path=?"); //$NON-NLS-1$
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
			lastAttemptToClose(pStmt);
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
	private long getGeneratedId(PreparedStatement pStmt) throws DbException {
		PreparedStatement pStmt1 = null;
		try {
			// Récupération de l'identifiant généré
			ResultSet rs = pStmt.getGeneratedKeys();
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			return rs.getLong(1);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.SQL_AUTOINCREMENT_FAILURE"), e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(pStmt1);
		}
	}

	/**
	 * Tries to close in a last attempt the {@link Statement}.
	 * 
	 * @param stmt
	 *            the {@link Statement} to close.
	 */
	private void lastAttemptToClose(Statement stmt) {
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
	private void lastAttemptClose(ResultSet rs) {
		if (rs != null)
			try {
				rs.close();
			} catch (Throwable ignored) {
			}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getRootTasksCount()
	 */
	public int getRootTasksCount() throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Request preparation
			pStmt = tx().prepareStatement(
					"select count(tsk_id) from TASK where tsk_path=''");

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			int result = rs.getInt(1);

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
			lastAttemptToClose(pStmt);
		}
	}

}
