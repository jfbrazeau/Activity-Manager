package org.activitymgr.core.dao;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.orm.IConverter;
import org.activitymgr.core.orm.IDAO;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public abstract class AbstractORMDAOImpl<TYPE> extends AbstractDAOImpl implements org.activitymgr.core.dao.IDAO<TYPE> {

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractORMDAOImpl.class);

	@Inject
	private IDAO<TYPE> wrapped;

	private Map<String, String> columnNamesRequestFragmentByTableAlias = new HashMap<String, String>();
	
	@Override
	public TYPE selectByPK(Object... pkValues) throws DAOException {
		try {
			return wrapped.selectByPK(tx(), pkValues);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public boolean deleteByPK(Object... pkValues) throws DAOException {
		try {
			return wrapped.deleteByPK(tx(), pkValues);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public boolean delete(TYPE instance) throws DAOException {
		try {
			return wrapped.delete(tx(), instance);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public int delete(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws DAOException {
		try {
			return wrapped.delete(tx(),
					whereClauseAttributeNames, whereClauseAttributeValues);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public TYPE[] selectAll() throws DAOException {
		try {
			return wrapped.selectAll(tx());
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public void dump(OutputStream out, String encoding,
			String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues, Object[] orderByClauseItems,
			int maxRows) throws DAOException {
		try {
			wrapped.dump(out, encoding, tx(),
					whereClauseAttributeNames, whereClauseAttributeValues,
					orderByClauseItems, maxRows);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public TYPE[] select(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues, Object[] orderByClauseItems,
			int maxRows) throws DAOException {
		try {
			return wrapped.select(tx(),
					whereClauseAttributeNames, whereClauseAttributeValues,
					orderByClauseItems, maxRows);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public TYPE update(TYPE value) throws DAOException {
		try {
			return wrapped.update(tx(), value);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public TYPE insert(TYPE value) throws DAOException {
		try {
			return wrapped.insert(tx(), value);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public long countAll() throws DAOException {
		try {
			return wrapped.countAll(tx());
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public long count(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws DAOException {
		try {
			return wrapped.count(tx(),
					whereClauseAttributeNames, whereClauseAttributeValues);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public String getColumnNamesRequestFragment(String tableAliasToUse) {
		String columnNamesRequestFragment = columnNamesRequestFragmentByTableAlias.get(tableAliasToUse);
		if (columnNamesRequestFragment == null) {
			columnNamesRequestFragment = wrapped.getColumnNamesRequestFragment(tableAliasToUse, true);
			columnNamesRequestFragmentByTableAlias.put(tableAliasToUse, columnNamesRequestFragment);
		}
		return columnNamesRequestFragment;
	}

	@Override
	public TYPE read(ResultSet rs, int fromIndex) {
		return wrapped.read(rs, fromIndex);
	}

	@Override
	public TYPE newInstance() {
		return wrapped.newInstance();
	}
	
	/**
	 * @return the active connection.
	 */
	protected Connection tx() {
		return super.tx();
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
			Calendar toDate, 
			boolean insertWhereClause, 
			String orderByClause)
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
		IntervalRequestHelper interval = new IntervalRequestHelper(fromDate, toDate);
		if (interval.hasIntervalCriteria()) {
			request.append(insertWhereClause ? " where" : " and");
			insertWhereClause = false;
			interval.appendIntervalCriteria(request);
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
		// Bind interval parameters
		paramIdx = interval.bindParameters(paramIdx, pStmt);
		return pStmt;
	}

}
