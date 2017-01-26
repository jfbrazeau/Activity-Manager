package org.activitymgr.core.dto.report;

import java.util.Calendar;

public enum ReportIntervalType {
	
	DAY(Calendar.DATE), WEEK(Calendar.WEEK_OF_YEAR), MONTH(Calendar.MONTH), YEAR(Calendar.YEAR);

	private int type;
	
	private ReportIntervalType(int type) {
		this.type = type;
	}

	public int getIntType() {
		return type;
	}
	
}
