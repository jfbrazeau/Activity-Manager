package org.activitymgr.core.dao;

import java.util.Calendar;

import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;


public interface IReportDAO {
	
	/**
	 * 
	 * @param start
	 * @param intervalType
	 * @param intervalCount
	 * @param rootTaskId
	 * @param taskDepth
	 * @param orderByContributor
	 *            <code>true</code> if the report must be sorted by contributor,
	 *            <code>false</code> if it must be sorted by task.
	 * @return
	 */
	Report buildReport(Calendar start, ReportIntervalType intervalType, int intervalCount, Long rootTaskId, int taskDepth, boolean byContributor, boolean orderByContributor);
	
}
