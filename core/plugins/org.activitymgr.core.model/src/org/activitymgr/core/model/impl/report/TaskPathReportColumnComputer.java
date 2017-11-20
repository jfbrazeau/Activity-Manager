package org.activitymgr.core.model.impl.report;

import java.io.StringWriter;

import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.dto.report.ReportItem;
import org.activitymgr.core.model.IReportColumnComputer;

public class TaskPathReportColumnComputer extends IReportColumnComputer.Impl {
	
	public TaskPathReportColumnComputer() {
		super("Path", false);
	}

	@Override
	public Object compute(ReportItem item) {
		// Path specific case
		StringWriter sw = new StringWriter();
		for (TaskSums cursor : item.getTasks()) {
			sw.append('/').append(cursor.getTask().getCode());
		}
		return sw.toString();
	}

}
