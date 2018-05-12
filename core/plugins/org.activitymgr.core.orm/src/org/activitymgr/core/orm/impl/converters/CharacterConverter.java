package org.activitymgr.core.orm.impl.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;

public class CharacterConverter implements IConverter<Character> {

	@Override
	public void bind(PreparedStatement stmt, int index, Character value) throws SQLException {
		stmt.setString(index, new String(new char[] { value }));
	}

	@Override
	public Character readValue(ResultSet rs, int index) throws SQLException {
		return rs.getString(index).charAt(0);
	}
	
	@Override
	public int getSQLType() {
		return Types.CHAR;
	}

}
