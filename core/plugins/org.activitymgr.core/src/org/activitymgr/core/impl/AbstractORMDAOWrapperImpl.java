package org.activitymgr.core.impl;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.activitymgr.core.DAOException;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.orm.IDAO;

import com.google.inject.Inject;
import com.google.inject.Provider;

public abstract class AbstractORMDAOWrapperImpl<TYPE> implements org.activitymgr.core.IORMDAOWrapper<TYPE> {

	public static class TaskDAOWrapper extends AbstractORMDAOWrapperImpl<Task> {
	}
	
	public static class ContributionDAOWrapper extends AbstractORMDAOWrapperImpl<Contribution> {
	}

	public static class CollaboratorDAOWrapper extends AbstractORMDAOWrapperImpl<Collaborator> {
	}

	public static class DurationDAOWrapper extends AbstractORMDAOWrapperImpl<Duration> {
	}

	@Inject
	private IDAO<TYPE> wrapped;

	/** Transaction provider */
	@Inject
	private Provider<Connection> tx;
	
	@Override
	public TYPE selectByPK(Object[] pkValue) throws DAOException {
		try {
			return wrapped.selectByPK(tx.get(), pkValue);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public boolean deleteByPK(TYPE instance) throws DAOException {
		try {
			return wrapped.deleteByPK(tx.get(), instance);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public int delete(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws DAOException {
		try {
			return wrapped.delete(tx.get(),
					whereClauseAttributeNames, whereClauseAttributeValues);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public TYPE[] selectAll() throws DAOException {
		try {
			return wrapped.selectAll(tx.get());
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
			wrapped.dump(out, encoding, tx.get(),
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
			return wrapped.select(tx.get(),
					whereClauseAttributeNames, whereClauseAttributeValues,
					orderByClauseItems, maxRows);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public TYPE update(TYPE value) throws DAOException {
		try {
			return wrapped.update(tx.get(), value);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public TYPE insert(TYPE value) throws DAOException {
		try {
			return wrapped.insert(tx.get(), value);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public long countAll() throws DAOException {
		try {
			return wrapped.countAll(tx.get());
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public long count(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws DAOException {
		try {
			return wrapped.count(tx.get(),
					whereClauseAttributeNames, whereClauseAttributeValues);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

}
