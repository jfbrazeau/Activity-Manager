package org.activitymgr.core;

import java.io.OutputStream;

public interface IORMDAOWrapper<TYPE> {

	public TYPE selectByPK(Object[] pkValue) throws DAOException;

	public boolean deleteByPK(TYPE instance) throws DAOException;

	public int delete(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws DAOException;

	public TYPE[] selectAll() throws DAOException;

	public void dump(OutputStream out, String encoding,
			String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues, Object[] orderByClauseItems,
			int maxRows) throws DAOException;

	public TYPE[] select(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues, Object[] orderByClauseItems,
			int maxRows) throws DAOException;

	public TYPE update(TYPE value) throws DAOException;

	public TYPE insert(TYPE value) throws DAOException;

	public long countAll() throws DAOException;

	public long count(String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws DAOException;

}