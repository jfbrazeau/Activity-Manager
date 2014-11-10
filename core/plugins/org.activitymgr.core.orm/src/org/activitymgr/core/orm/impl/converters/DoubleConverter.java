package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.activitymgr.core.orm.IConverter;

public class DoubleConverter implements IConverter<Double> {

	@Override
	public void bind(PreparedStatement stmt, int index, Double value) throws SQLException {
		stmt.setDouble(index, value);
	}

	@Override
	public Double readValue(ResultSet rs, int index) throws SQLException {
		return rs.getDouble(index);
	}
	
}
