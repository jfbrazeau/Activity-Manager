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
import java.util.List;
import java.util.Properties;

import org.activitymgr.core.DbException;
import org.activitymgr.core.DbTransaction;
import org.activitymgr.core.IDbMgr;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;
import org.activitymgr.core.orm.DAOFactory;
import org.activitymgr.core.orm.IDAO;
import org.activitymgr.core.orm.query.AscendantOrderByClause;
import org.activitymgr.core.orm.query.DescendantOrderByClause;
import org.activitymgr.core.orm.query.InStatement;
import org.activitymgr.core.orm.query.LikeStatement;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Classe offrant les services de base de persistence de l'application.
 * TODO 2236 -> 1865 -> 1558
 */
public class DbMgrImpl implements IDbMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(DbMgrImpl.class);

	/** Formatteur de date */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

	/** Transaction provider */
	private Provider<DbTransaction> tx;
	
	/** Database task mapper */
	private IDAO<Collaborator> collaboratorMapper;

	/** Database task mapper */
	private IDAO<Task> taskMapper;

	/** Database task mapper */
	private IDAO<Duration> durationMapper;

	/** Database task mapper */
	private IDAO<Contribution> contributionMapper;

	/**
	 * Default constructor.
	 * 
	 * @param tx
	 *            transaction provider.
	 */
	@Inject
	public DbMgrImpl(Provider<DbTransaction> tx) {
		this.tx = tx;
		try {
			Properties props = new Properties();
			props.load(DbMgrImpl.class.getResourceAsStream("mapping.properties"));
			DAOFactory mapping = new DAOFactory(props);
			// TODO Move
			collaboratorMapper = (IDAO<Collaborator>) mapping.getMapper(Collaborator.class);
			taskMapper = (IDAO<Task>) mapping.getMapper(Task.class);
			durationMapper = (IDAO<Duration>) mapping.getMapper(Duration.class);
			contributionMapper = (IDAO<Contribution>) mapping.getMapper(Contribution.class);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
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
		try {
			return collaboratorMapper.insert(tx().getConnection(), newCollaborator);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_CREATION_FAILUE", newCollaborator.getLogin()), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			return contributionMapper.insert(tx().getConnection(), newContribution);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTION_CREATION_FAILUE"), e); //$NON-NLS-1$
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
		try {
			return durationMapper.insert(tx().getConnection(), newDuration);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.DURATION_CREATION_FAILUE", newDuration), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			// Mise à jour du chemin de la tâche
			String parentPath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
			newTask.setPath(parentPath);

			// Génération du numéro de la tâche
			byte taskNumber = newTaskNumber(parentPath);
			newTask.setNumber(taskNumber);

			return taskMapper.insert(tx().getConnection(), newTask);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_CREATION_FAILURE", newTask.getName()), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			return contributionMapper.count(tx().getConnection(), new String[] { "durationId" }, new Object[] { duration.getId()}) > 0;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.SQL_DURATION_CHECK_FAILURE", duration), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.IDbMgr#subTasksCount(long)
	 */
	public int getSubTasksCount(long parentTaskId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Request preparation
			pStmt = tx().prepareStatement(
					"select theTask.tsk_id, count(subTask.tsk_id)"
				+ " from TASK as theTask left join TASK as subTask on subTask.tsk_path = concat(theTask.tsk_path, theTask.tsk_number) where theTask.tsk_id=?");
			pStmt.setLong(1, parentTaskId);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			if (!rs.next())
				throw new DbException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			int result = rs.getInt(2);

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
	 * @see org.activitymgr.core.IDbMgr#getCollaborator(long)
	 */
	@Override
	public Collaborator getCollaborator(long collaboratorId) throws DbException {
		try {
			return collaboratorMapper.selectByPK(tx().getConnection(), new Object[] { collaboratorId });
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_SELECTION_BY_ID_FAILURE", new Long(collaboratorId)), e); //$NON-NLS-1$ //$NON-NLS-2$
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
	@Deprecated
	private Collaborator rsToCollaborator(ResultSet rs) throws SQLException {
		Collaborator collaborator = new Collaborator();
		collaborator.setId(rs.getLong(1));
		collaborator.setLogin(rs.getString(2));
		collaborator.setFirstName(rs.getString(3));
		collaborator.setLastName(rs.getString(4));
		collaborator.setIsActive(rs.getBoolean(5));
		return (Collaborator) collaborator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getCollaborator(java.lang.String)
	 */
	@Override
	public Collaborator getCollaborator(String login) throws DbException {
		try {
			Collaborator[] collaborators = collaboratorMapper.select(tx().getConnection(), new String[] { "login" }, new Object[] { login }, null, -1);
			return collaborators.length > 0 ? collaborators[0] : null;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_SELECTION_BY_LOGIN_FAILURE", login), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			String[] whereClauseAttrNames = onlyActiveCollaborators ? new String[] { "isActive" } : null;
			Object[] whereClauseAttrValues = onlyActiveCollaborators ? new Object[] { Boolean.TRUE } : null;
			String orderByClauseFieldName = null;
			switch (orderByClauseFieldIndex) {
			case Collaborator.ID_FIELD_IDX:
				orderByClauseFieldName = "id"; //$NON-NLS-1$
				break;
			case Collaborator.LOGIN_FIELD_IDX:
				orderByClauseFieldName = "login"; //$NON-NLS-1$
				break;
			case Collaborator.FIRST_NAME_FIELD_IDX:
				orderByClauseFieldName = "firstName"; //$NON-NLS-1$
				break;
			case Collaborator.LAST_NAME_FIELD_IDX:
				orderByClauseFieldName = "lastName"; //$NON-NLS-1$
				break;
			case Collaborator.IS_ACTIVE_FIELD_IDX:
				orderByClauseFieldName = "isActive"; //$NON-NLS-1$
				break;
			default:
				throw new DbException(
						Strings.getString(
								"DbMgr.errors.UNKNOWN_FIELD_INDEX", new Integer(orderByClauseFieldIndex)), null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			Object[] orderByClause = new Object[] { ascendantSort ? new AscendantOrderByClause(orderByClauseFieldName) : new DescendantOrderByClause(orderByClauseFieldName)};
			return collaboratorMapper.select(tx().getConnection(), whereClauseAttrNames, whereClauseAttrValues, orderByClause, -1);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.COLLABORATORS_SELECTION_FAILURE"), e); //$NON-NLS-1$
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
	// TODO rely on ORM
	@Deprecated
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
		try {
			return durationMapper.selectByPK(tx().getConnection(), new Object[] { durationId });
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.DURATION_SELECTION_BY_ID", new Long(durationId)), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getDurations(boolean)
	 */
	@Override
	public Duration[] getDurations(boolean onlyActive)
			throws DbException {
		try {
			String[] whereClauseAttributeNames = onlyActive ? new String[] { "isActive" } : null;
			Object[] whereClauseAttributeValues = onlyActive ? new Object[] { Boolean.TRUE } : null;
			return durationMapper.select(tx().getConnection(), whereClauseAttributeNames, whereClauseAttributeValues, new Object[] { new AscendantOrderByClause("id") }, -1);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.DURATIONS_SELECTION_FAILURE"), e); //$NON-NLS-1$
		}
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
		try {
			return taskMapper.select(tx().getConnection(), new String[] { "path" }, new Object[] { path }, new Object[] { new AscendantOrderByClause("number") }, -1);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_SELECTION_BY_PATH", path), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getSubTasks(org.activitymgr.core.beans.Task)
	 */
	@Override
	public Task[] getSubTasks(Task parentTask) throws DbException {
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
		try {
			return taskMapper.selectByPK(tx().getConnection(), new Object[] { taskId });
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_ID_FAILURE"), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTasks(long[])
	 */
	@Override
	public Task[] getTasks(long[] tasksIds) throws DbException {
		List<Task> result = new ArrayList<Task>();
		try {
			if (tasksIds != null && tasksIds.length != 0) {
				// The task id array is cut in sub arrays of maximum 250 tasks
				List<Object[]> tasksIdsSubArrays = new ArrayList<Object[]>();
				for (int i = 0; i < tasksIds.length; i += 250) {
					Object[] subArray = new Object[Math.min(250, tasksIds.length
							- i)];
					for (int j = 0; j < subArray.length; j++) {
						subArray[j] = tasksIds[i + j];
					}
					tasksIdsSubArrays.add(subArray);
				}

				// Then a loop is performed over the sub arrays
				for (Object[] tasksIdsSubArray : tasksIdsSubArrays) {
					Task[] tasks = taskMapper.select(tx().getConnection(), new String[] { "id" }, new Object[] { new InStatement(tasksIdsSubArray) }, new Object[] { new AscendantOrderByClause("number") }, -1);
					result.addAll(Arrays.asList(tasks));
				}
			}
			// Retour du résultat
			// FIXME
			return (Task[]) result.toArray(new Task[result.size()]);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_ID_FAILURE"), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTask(java.lang.String, byte)
	 */
	@Override
	public Task getTask(String taskPath, byte taskNumber) throws DbException {
		try {
			Task[] tasks = taskMapper.select(tx().getConnection(), new String[] { "path", "number" }, new Object[] { taskPath, taskNumber }, null, -1);
			return tasks.length > 0 ? tasks[0] : null;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.TASK_SELECTION_BY_NUMBER_FROM_PATH_FAILURE", new Byte(taskNumber), taskPath), e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		try {
			Task[] tasks = taskMapper.select(tx().getConnection(), new String[] { "path", "code" }, new Object[] { taskPath, taskCode }, null, -1);
			return tasks.length > 0 ? tasks[0] : null;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_SELECTION_BY_CODE_FAILURE", taskCode), e); //$NON-NLS-1$ //$NON-NLS-2$
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
			// FIXME
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

			/**
			 * Calcul de la partie indépendante des contributions (budget /
			 * consommation initiale / reste à faire
			 */
			pStmt = tx()
					.prepareStatement(
							"select sum(tsk_budget), sum(tsk_initial_cons), sum(tsk_todo) from TASK where concat(tsk_path, tsk_number)=? or (tsk_path like ?)"); //$NON-NLS-1$
			String path = (task == null ? "" : task.getFullPath());
			pStmt.setString(1, path);
			pStmt.setString(2, path
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
		try {
			collaboratorMapper.delete(tx().getConnection(), new String[] { "id" }, new Object[] { collaborator.getId() });
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_DELETION_FAILURE", collaborator.getLogin()), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			contributionMapper.deleteByPK(tx().getConnection(), contribution);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTION_DELETION_FAILURE"), e); //$NON-NLS-1$
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
		try {
			durationMapper.deleteByPK(tx().getConnection(), duration);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.DURATION_DELETION_FAILURE", new Long(duration.getId())), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			// Delete sub tasks
			taskMapper.delete(tx().getConnection(), new String[] { "path" }, new Object[] { new LikeStatement(task.getFullPath() + "%") });
			// Delete the task
			taskMapper.deleteByPK(tx().getConnection(), task);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_DELETION_FAILURE", task.getName()), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			return collaboratorMapper.update(tx().getConnection(), collaborator);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.COLLABORATOR_UPDATE_FAILURE", collaborator.getLogin()), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			return contributionMapper.update(tx().getConnection(), contribution);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.CONTRIBUTION_UPDATE_FAILURE"), e); //$NON-NLS-1$
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
		try {
			return durationMapper.update(tx().getConnection(), duration);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString(
							"DbMgr.errors.DURATION_UPDATE_FAILURE", new Long(duration.getId())), e); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			return taskMapper.update(tx().getConnection(), task);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(Strings.getString(
					"DbMgr.errors.TASK_UPDATE_FAILURE", task.getName()), e); //$NON-NLS-1$ //$NON-NLS-2$
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getRootTasksCount()
	 */
	public int getRootTasksCount() throws DbException {
		try {
			return (int) taskMapper.count(tx().getConnection(), new String[] { "path" }, new Object[] { "" });
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DbException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_ID_FAILURE"), e); //$NON-NLS-1$ //$NON-NLS-2$
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

}
