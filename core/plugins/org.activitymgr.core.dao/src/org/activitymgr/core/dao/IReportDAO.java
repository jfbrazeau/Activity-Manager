package org.activitymgr.core.dao;

import java.util.Calendar;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;

public interface IReportDAO {

	/**
	 * 
	 * @param start
	 * @param intervalType
	 * @param intervalCount
	 * @param rootTask
	 * @param taskDepth
	 * @param onlyKeepTasksWithContributions
	 *            <code>true</code> if the report must only keep tasks with
	 *            contributions. Has no effect if <code>taskDepth == 0</code> or
	 *            if <code>contributorCentricMode == true</code>.
	 * @param byContributor
	 *            tells whether contributors must appear in the result.
	 * @param contributorCentricMode
	 *            <code>true</code> if the report must be sorted by contributor,
	 *            <code>false</code> if it must be sorted by task (ignored if
	 *            <code>byContributor</code> is <code>false</code>).
	 * @param contributorIds
	 *            contributor identifiers (optional).
	 * @param orderContributorsBy
	 *            fields to use to order contributors (ignored if
	 *            <code>byContributor</code> is <code>false</code>).
	 * @return
	 */
	Report buildReport(Calendar start, ReportIntervalType intervalType,
			int intervalCount, Task rootTask, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds,
			String[] orderContributorsBy);

}
