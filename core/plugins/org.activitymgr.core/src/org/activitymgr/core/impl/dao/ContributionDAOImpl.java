package org.activitymgr.core.impl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSums;
import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

public class ContributionDAOImpl extends AbstractORMDAOImpl<Contribution> implements
		IContributionDAO {

	/** Logger */
	private static Logger log = Logger.getLogger(ContributionDAOImpl.class);

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
			Calendar fromDate, Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate,
					getColumnNamesRequestFragment());

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Extraction du résultat
			ArrayList<Contribution> list = new ArrayList<Contribution>();
			while (rs.next()) {
				list.add(read(rs, 1));
			}
			Contribution[] result = (Contribution[]) list.toArray(new Contribution[list.size()]);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
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
			Calendar fromDate, Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the SQL request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate, "sum(ctb_duration)");

			// Exécution de le requête et extraction du résultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DAOException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			long contributionSums = rs.getLong(1);
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return contributionSums;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
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
			Calendar fromDate, Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the SQL request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate, "count(ctb_duration)");

			// Exécution de le requête et extraction du résultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DAOException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			int count = rs.getInt(1);
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return count;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
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
	 * org.activitymgr.core.IDbMgr#getTaskSums(org.activitymgr.core.beans.Task,
	 * java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public TaskSums getTaskSums(Task task, Calendar fromDate, Calendar toDate)
			throws DAOException {
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
				throw new DAOException(
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
				throw new DAOException(
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
					throw new DAOException(
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
			throw new DAOException(
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

}