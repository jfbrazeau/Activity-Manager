package org.activitymgr.core.report;

import java.util.Calendar;

import org.activitymgr.core.AbstractModelTestCase;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.ModelException;

public class XLSReportTest extends AbstractModelTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testOverflow() throws ModelException {
		Calendar start = cal(2017, 1, 1);
		// No overflow, dry run
		getModelMgr().buildReport(start, ReportIntervalType.DAY, 100, null, 1,
				false, false, false, null, new String[] { "task.path" }, true,
				true);
		// No overflow, dry run = false
		getModelMgr().buildReport(start, ReportIntervalType.DAY, 100, null, 1,
				false, false, false, null, new String[] { "task.path" }, true,
				false);
		try {
			// Overflow (255 intervals), dry run
			getModelMgr().buildReport(start, ReportIntervalType.DAY, 255, null,
					1, false, false, false, null, new String[] { "task.path" },
					true, true);
			fail("An overflow should have occured");
		} catch (ModelException e) {

		}
		try {
			// Overflow (255 intervals), dry run = false
			getModelMgr().buildReport(start, ReportIntervalType.DAY, 255, null,
					1, false, false, false, null, new String[] { "task.path" },
					true, true);
			fail("An overflow should have occured");
		}
		catch (ModelException e){
			
		}
	}

}
