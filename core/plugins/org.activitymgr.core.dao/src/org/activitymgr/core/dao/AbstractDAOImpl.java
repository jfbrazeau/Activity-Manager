package org.activitymgr.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.impl.dao.TaskDAOImpl;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AbstractDAOImpl {
	
	/** Formatteur de date */
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

	/** Logger */
	private static Logger log = Logger.getLogger(TaskDAOImpl.class);

	/** Transaction provider */
	@Inject
	private Provider<Connection> tx;
	
	/**
	 * @return the active connection.
	 */
	protected Connection tx() {
		return tx.get();
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
	protected PreparedStatement buildIntervalRequest(StringBuffer request,
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

	/**
	 * Tries to close in a last attempt the {@link Statement}.
	 * 
	 * @param stmt
	 *            the {@link Statement} to close.
	 */
	protected void lastAttemptToClose(Statement stmt) {
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
	protected void lastAttemptClose(ResultSet rs) {
		if (rs != null)
			try {
				rs.close();
			} catch (Throwable ignored) {
			}
	}


}
