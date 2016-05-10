package org.activitymgr.core.impl.dao;

import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSearchFilter;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

/**
 * @author jbrazeau
 *
 */
public class TaskDAOImpl extends AbstractORMDAOImpl<Task> implements ITaskDAO {

	/** Logger */
	private static Logger log = Logger.getLogger(TaskDAOImpl.class);
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.IDbMgr#subTasksCount(long)
	 */
	@Override
	public int getSubTasksCount(long parentTaskId) throws DAOException {
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
				throw new DAOException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			int result = rs.getInt(2);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(e.getMessage(), e);
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	@Override
	public Task[] getSubTasks(String parentTaskPath, String filter) {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			StringWriter buf = new StringWriter();
			buf.append("select distinct ").append(getColumnNamesRequestFragment("subtask")).append(" from TASK as subtask");
			buf.append(" inner join TASK filteredTask on (");
			buf.append("   left(concat(filteredTask.tsk_path, filteredTask.tsk_number), length(subtask.tsk_path) + 2) = concat(subtask.tsk_path, subtask.tsk_number)");
			buf.append("   or left(concat(subtask.tsk_path, subtask.tsk_number), length(filteredTask.tsk_path) + 2) = concat(filteredTask.tsk_path, filteredTask.tsk_number)");
			buf.append(" )");
			buf.append(" where subtask.tsk_path=? and (filteredTask.tsk_name like ? or filteredTask.tsk_code like ?) order by subtask.tsk_number");
			
			// Request preparation
			pStmt = tx().prepareStatement(buf.toString());
			pStmt.setString(1, parentTaskPath);
			String sqlFilter = "%" + filter + "%";
			pStmt.setString(2, sqlFilter);
			pStmt.setString(3, sqlFilter);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			Collection<Task> result = new ArrayList<Task>();
			while (rs.next()) {
				result.add(read(rs, 1));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return (Task[]) result.toArray(new Task[result.size()]);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(e.getMessage(), e);
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	@Override
	public Task getFirstTaskMatching(String filter) {
		// select  distinct st.TSK_ID, st.TSK_PATH, st.TSK_NUMBER, st.TSK_CODE from task st inner join TASK t on left(concat(t.tsk_path, t.tsk_number), length(concat(st.tsk_path, st.tsk_number))) = concat(st.tsk_path, st.tsk_number) where st.tsk_path='01090304' and t.tsk_name like concat('%', 'CCAP', '%') order by st.tsk_number; 
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			StringWriter buf = new StringWriter();
			buf.append("select ").append(getColumnNamesRequestFragment("t")).append(" from TASK as t");
			buf.append(" where t.tsk_name like ? or t.tsk_code like ? order by t.tsk_path, t.tsk_number");
			
			// Request preparation
			pStmt = tx().prepareStatement(buf.toString());
			String sqlFilter = "%" + filter + "%";
			pStmt.setString(1, sqlFilter);
			pStmt.setString(2, sqlFilter);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Préparation du résultat
			Task result = null;
			if (rs.next()) {
				result = read(rs, 1);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(e.getMessage(), e);
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.activitymgr.core.IDbMgr#getTasks(org.activitymgr.core.beans.
	 * TaskSearchFilter)
	 */
	@Override
	public long[] getTaskIds(TaskSearchFilter filter) throws DAOException {
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
				throw new DAOException(
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
				throw new DAOException(
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
				throw new DAOException(
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
			return taskIds;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString("DbMgr.errors.TASKS_SELECTION_FAILURE"), e); //$NON-NLS-1$
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
	public long[] getContributedTaskIds(Collaborator contributor, Calendar fromDate,
			Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer();
			request.append("select distinct ctb_task, tsk_path, tsk_number from CONTRIBUTION, TASK where ctb_task=tsk_id");
			pStmt = buildIntervalRequest(
					request, contributor, null, fromDate, toDate, false, "tsk_path, tsk_number");

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
			return taskIds;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_COLLABORATOR_FAILURE"), e); //$NON-NLS-1$
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
	public byte newTaskNumber(String path) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Recherche du max
			pStmt = tx().prepareStatement(
					"select max(tsk_number) from TASK where tsk_path=?"); //$NON-NLS-1$
			pStmt.setString(1, path);
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DAOException(
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
			throw new DAOException(Strings.getString(
					"DbMgr.errors.TASK_NUMBER_COMPUTATION_FAILURE", path), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.dao.ITaskDAO#getTasksSums(java.lang.Long, java.lang.String)
	 */
	@Override
	public List<TaskSums> getTasksSums(Long taskId, String tasksPath) throws DAOException {
		if (taskId != null && tasksPath != null) {
			throw new IllegalStateException("Both task Id and task path cannot be specified");
		}
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			List<TaskSums> result = new ArrayList<TaskSums>();

			/**
			 * Budget, initialy consummed, etc sums computation (all what is independant
			 * from contributions)
			 */
			StringBuffer request = new StringBuffer()
					.append("select ")
					.append("sum(leaftask.tsk_budget), sum(leaftask.tsk_initial_cons), sum(leaftask.tsk_todo), count(leaftask.tsk_id), ")
					.append(getColumnNamesRequestFragment("maintask"))
					.append(" from TASK maintask, TASK leaftask ")
					.append("where ");
			// Task id case
			if (taskId != null) {
				request.append("maintask.tsk_id=?");
			}
			// Task path case
			else {
				request.append("maintask.tsk_path=?");
			}
			if (taskId != null || tasksPath != null) {
				request.append(" and ");
			}
			request.append("(maintask.tsk_id=leaftask.tsk_id or leaftask.tsk_path like concat(maintask.tsk_path, maintask.tsk_number, '%'))")
				.append(" group by maintask.tsk_id ")
				.append(" order by maintask.tsk_number");
			pStmt = tx().prepareStatement(request.toString());
			int paramIdx = 1;
			if (taskId != null) {
				pStmt.setLong(paramIdx++, taskId);
			}
			if (tasksPath != null) {
				pStmt.setString(paramIdx++, tasksPath);
			}
			rs = pStmt.executeQuery();
			
			while (rs.next()) {
				TaskSums sums = new TaskSums();
				sums.setBudgetSum(rs.getLong(1));
				sums.setInitiallyConsumedSum(rs.getLong(2));
				sums.setTodoSum(rs.getLong(3));
				sums.setLeaf(rs.getLong(4) == 1);
				Task task = read(rs, 5);
				sums.setTask(task);
				result.add(sums);
			}
			// Close the statement
			pStmt.close();
			pStmt = null;
			
			// Return the result
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString(
							"DbMgr.errors.TASK_SUMS_COMPUTATION_FAILURE", taskId != null ? taskId : tasksPath), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			try {
				if (pStmt != null)
					pStmt.close();
			} catch (Throwable ignored) {
			}
		}
	}

}