package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.activitymgr.core.orm.IConverter;

public class StringConverter implements IConverter<String> {

	@Override
	public void bind(PreparedStatement stmt, int index, String value) throws SQLException {
		stmt.setString(index, value);
	}

	@Override
	public String readValue(ResultSet rs, int index) throws SQLException {
		return rs.getString(index);
	}
	
}
