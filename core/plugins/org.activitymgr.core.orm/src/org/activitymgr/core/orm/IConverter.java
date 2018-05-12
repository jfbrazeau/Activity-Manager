package org.activitymgr.core.orm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface IConverter<TYPE> {
	
	void bind(PreparedStatement stmt, int index, TYPE value) throws SQLException;
	
	TYPE readValue(ResultSet rs, int index) throws SQLException;

	int getSQLType();

}
