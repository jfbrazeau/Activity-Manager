package org.activitymgr.core.dao;

import java.io.OutputStream;
import java.sql.ResultSet;

public interface IDAO<TYPE> {

	public TYPE selectByPK(Object... pkValues) throws DAOException;

	public boolean delete(TYPE instance) throws DAOException;

	public boolean deleteByPK(Object... pkValues) throws DAOException;

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

	public String getColumnNamesRequestFragment();
	
	public TYPE read(ResultSet rs, int fromIndex);

}