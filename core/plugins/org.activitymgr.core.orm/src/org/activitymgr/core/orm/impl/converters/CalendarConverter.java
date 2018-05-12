package org.activitymgr.core.orm.impl.converters;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.core.orm.IConverter;

public class CalendarConverter implements IConverter<Calendar> {

	@Override
	public void bind(PreparedStatement stmt, int index, Calendar value) throws SQLException {
		stmt.setDate(index, new Date(value.getTime().getTime()));
	}

	@Override
	public Calendar readValue(ResultSet rs, int index) throws SQLException {
		Date date = rs.getDate(index);
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.setTime(date);
		return gregorianCalendar;
	}
	
	@Override
	public int getSQLType() {
		return Types.DATE;
	}

}
