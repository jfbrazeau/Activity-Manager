package org.activitymgr.core.orm.impl.converters;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;

public class BigDecimalConverter implements IConverter<BigDecimal> {

	@Override
	public void bind(PreparedStatement stmt, int index, BigDecimal value) throws SQLException {
		stmt.setBigDecimal(index, value);
	}

	@Override
	public BigDecimal readValue(ResultSet rs, int index) throws SQLException {
		return rs.getBigDecimal(index);
	}
	
	@Override
	public int getSQLType() {
		return Types.DECIMAL;
	}

}
