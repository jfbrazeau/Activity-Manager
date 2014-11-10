package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.activitymgr.core.orm.IConverter;

public class FloatConverter implements IConverter<Float> {

	@Override
	public void bind(PreparedStatement stmt, int index, Float value) throws SQLException {
		stmt.setFloat(index, value);
	}

	@Override
	public Float readValue(ResultSet rs, int index) throws SQLException {
		return rs.getFloat(index);
	}
	
}
