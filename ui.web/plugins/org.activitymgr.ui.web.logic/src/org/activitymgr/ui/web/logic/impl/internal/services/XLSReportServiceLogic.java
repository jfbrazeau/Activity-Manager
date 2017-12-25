package org.activitymgr.ui.web.logic.impl.internal.services;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.activitymgr.core.dto.Collaborator;
import org.apache.poi.ss.usermodel.Workbook;

public class XLSReportServiceLogic extends AbstractReportServiceLogic {

	@Override
	public String getPath() {
		return "/report/xls";
	}

	@Override
	protected void doService(Collaborator connected, Request request,
			Response response, Workbook report) throws IOException {
		response.setContentType("application/vnd.ms-excel");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
		String name = "am-report-" + sdf.format(new Date()) + ".xls";
		response.addHeader("Content-Disposition", "attachment; filename="
				+ name);
		report.write(response.getOutputStream());
	}
}
