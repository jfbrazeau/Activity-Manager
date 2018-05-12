package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;

public class IntegerConverter implements IConverter<Integer> {

	@Override
	public void bind(PreparedStatement stmt, int index, Integer value) throws SQLException {
		stmt.setInt(index, value);
	}

	@Override
	public Integer readValue(ResultSet rs, int index) throws SQLException {
		return rs.getInt(index);
	}
	
	@Override
	public int getSQLType() {
		return Types.INTEGER;
	}

}
