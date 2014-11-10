package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.activitymgr.core.orm.IConverter;

public class LongConverter implements IConverter<Long> {

	@Override
	public void bind(PreparedStatement stmt, int index, Long value) throws SQLException {
		stmt.setLong(index, value);
	}

	@Override
	public Long readValue(ResultSet rs, int index) throws SQLException {
		return rs.getLong(index);
	}
	
}
