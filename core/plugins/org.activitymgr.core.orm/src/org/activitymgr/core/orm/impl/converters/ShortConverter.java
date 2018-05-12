package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;

public class ShortConverter implements IConverter<Short> {

	@Override
	public void bind(PreparedStatement stmt, int index, Short value) throws SQLException {
		stmt.setShort(index, value);
	}

	@Override
	public Short readValue(ResultSet rs, int index) throws SQLException {
		return rs.getShort(index);
	}
	
	@Override
	public int getSQLType() {
		return Types.SMALLINT;
	}

}
