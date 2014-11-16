package org.activitymgr.core.impl.dao;

import java.io.OutputStream;
import java.sql.SQLException;

import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.orm.IDAO;

import com.google.inject.Inject;

public abstract class AbstractORMDAOImpl<TYPE> extends AbstractDAOImpl implements org.activitymgr.core.dao.IDAO<TYPE> {

	@Inject
	private IDAO<TYPE> wrapped;

	@Override
	public TYPE selectByPK(Object[] pkValue) throws DAOException {
		try {
			return wrapped.selectByPK(tx(), pkValue);
		} catch (SQLException e) {
			throw new DAOException(null, e);
		}
	}

	@Override
	public boolean deleteByPK(TYPE instance) throws DAOException {
		try {
			return wrapped.deleteByPK(tx(), instance);
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

}
