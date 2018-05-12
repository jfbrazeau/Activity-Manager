package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;

public class ByteConverter implements IConverter<Byte> {

	@Override
	public void bind(PreparedStatement stmt, int index, Byte value) throws SQLException {
		stmt.setByte(index, value);
	}

	@Override
	public Byte readValue(ResultSet rs, int index) throws SQLException {
		return rs.getByte(index);
	}
	
	@Override
	public int getSQLType() {
		return Types.TINYINT;
	}

}
