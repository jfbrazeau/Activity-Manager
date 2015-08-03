package org.activitymgr.core.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class IntervalRequestHelper {
	
	/** Formatteur de date */
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

	private Calendar fromDate;

	private Calendar toDate;

	private String fromDateStr;

	private String toDateStr;

	public IntervalRequestHelper(Calendar fromDate, Calendar toDate) {
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.fromDateStr = fromDate != null ? sdf.format(fromDate.getTime())
				: null;
		this.toDateStr = toDate != null ? sdf.format(toDate.getTime()) : null;
	}

	public boolean hasIntervalCriteria() {
		return fromDate != null || toDate != null;
	}
	
	public void appendIntervalCriteria(StringBuffer request) {
		if (hasIntervalCriteria()) {
			request.append(" (ctb_year*10000 + ( ctb_month*100 + ctb_day ))");
			// If both dates are specified
			if (fromDate != null && toDate != null) {
				if (!fromDateStr.equals(toDateStr)) {
					request.append(" between ? and ?");
				} else {
					request.append(" = ?");
				}
			}
			// Else if only 'from' specified (toDate == null)
			else if (fromDate != null) {
				request.append(" >= ?");
			}
			// Else if only 'to' specified (fromDate == null)
			else {
				request.append(" <= ?");
			}
		}
	}

	public int bindParameters(final int startIdx, PreparedStatement pStmt) throws SQLException {
		int index = startIdx;
		if (hasIntervalCriteria()) {
			// If both dates are specified
			if (fromDate != null && toDate != null) {
				if (!fromDateStr.equals(toDateStr)) {
					pStmt.setString(index++, fromDateStr);
					pStmt.setString(index++, toDateStr);
				} else {
					pStmt.setString(index++, fromDateStr);
				}
			}
			// Else if only 'from' specified (toDate == null)
			else if (fromDate != null) {
				pStmt.setString(index++, fromDateStr);
			}
			// Else if only 'to' specified (fromDate == null)
			else {
				pStmt.setString(index++, toDateStr);
			}
		}
		return index;
	}

}
