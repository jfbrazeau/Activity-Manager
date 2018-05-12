package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;

public class BooleanConverter implements IConverter<Boolean> {

	@Override
	public void bind(PreparedStatement stmt, int index, Boolean value) throws SQLException {
		stmt.setBoolean(index, value);
	}

	@Override
	public Boolean readValue(ResultSet rs, int index) throws SQLException {
		return rs.getBoolean(index);
	}
	
	@Override
	public int getSQLType() {
		return Types.BOOLEAN;
	}

}
