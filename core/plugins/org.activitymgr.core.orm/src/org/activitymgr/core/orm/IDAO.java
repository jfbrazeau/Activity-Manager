package org.activitymgr.core.orm;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface IDAO<TYPE> {

	public TYPE selectByPK(Connection con, Object... pkValue)
			throws SQLException;

	public boolean delete(Connection con, TYPE instance)
			throws SQLException;

	public boolean deleteByPK(Connection con, Object... pkValue)
			throws SQLException;

	public int delete(Connection con, String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws SQLException;

	public TYPE[] selectAll(Connection con) throws SQLException;

	public void dump(OutputStream out, String encoding, Connection con,
			String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues, Object[] orderByClauseItems,
			int maxRows) throws SQLException;

	public TYPE[] select(Connection con, String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues, Object[] orderByClauseItems,
			int maxRows) throws SQLException;

	public TYPE update(Connection con, TYPE value) throws SQLException;

	public TYPE insert(Connection con, TYPE value) throws SQLException;

	public long countAll(Connection con) throws SQLException;

	public long count(Connection con, String[] whereClauseAttributeNames,
			Object[] whereClauseAttributeValues) throws SQLException;

	public String getColumnNamesRequestFragment(String tableAliasToUse, boolean includePK);

	public String getColumnName(String attributeName);
	
	public TYPE read(ResultSet rs, int fromIndex);

	public TYPE newInstance();

}

