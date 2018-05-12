package org.activitymgr.core.dto.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;
import org.activitymgr.core.util.StringHelper;

public class TaskNumberConverter implements IConverter<Byte> {

	@Override
	public void bind(PreparedStatement stmt, int index, Byte value)
			throws SQLException {
		stmt.setString(index, StringHelper.toHex(value));
	}

	@Override
	public Byte readValue(ResultSet rs, int index) throws SQLException {
		return StringHelper.toByte(rs.getString(index));
	}

	@Override
	public int getSQLType() {
		return Types.VARCHAR;
	}

}
