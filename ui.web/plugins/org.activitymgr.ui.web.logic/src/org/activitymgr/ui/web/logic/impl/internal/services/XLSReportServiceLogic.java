package org.activitymgr.ui.web.logic.impl.internal.services;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.impl.AbstractServiceLogic;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.inject.Inject;

public class XLSReportServiceLogic extends AbstractServiceLogic {

	@Inject
	private IModelMgr modelMgr;

	@Override
	public String getPath() {
		return "/xlsreport";
	}

	@Override
	protected void doService(Request parameters, Response response)
			throws ModelException, IOException {
		response.setContentType("application/vnd.ms-excel");
		response.addHeader("Content-Disposition",
				"attachment; filename=MyFileName.xls");

		Calendar start = null;
		String startParam = parameters.getParameter("start");
		if (startParam != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			start = Calendar.getInstance();
			try {
				start.setTime(sdf.parse(startParam));
			} catch (ParseException e) {
				throw new IllegalArgumentException("start date is invalid", e);
			}
		}

		ReportIntervalType intervalType = ReportIntervalType.MONTH;
		String intervalTypeParam = parameters.getParameter("intervalType");
		if (intervalTypeParam != null) {
			intervalType = ReportIntervalType.valueOf(intervalTypeParam);
		}

		Integer intervalCount = null;
		String intervalCountParam = parameters.getParameter("intervalCount");
		if (intervalCountParam != null) {
			intervalCount = Integer.parseInt(intervalCountParam);
		}

		Long rootTaskId = null;
		String rootTaskIdParam = parameters.getParameter("rootTaskId");
		if (rootTaskIdParam != null) {
			rootTaskId = Long.parseLong(rootTaskIdParam);
		}

		int taskDepth = 1;
		String taskDepthParam = parameters.getParameter("taskDepth");
		if (taskDepthParam != null) {
			taskDepth = Integer.parseInt(taskDepthParam);
		}

		boolean onlyKeepTasksWithContributions = "true".equals(parameters
				.getParameter("onlyKeepTasksWithContributions"));

		String byContributorParam = parameters
				.getParameter("byContributor");
		boolean byContributor = byContributorParam == null
				|| "true".equals(byContributorParam);

		boolean contributorCentricMode = "true".equals(parameters
				.getParameter("contributorCentricMode"));

		long[] contributorIds = null;
		String[] contributorIdsParam = parameters
				.getListParameter("contributorIds");
		if (contributorIdsParam != null) {
			contributorIds = new long[contributorIdsParam.length];
			for (int i = 0; i < contributorIdsParam.length; i++) {
				contributorIds[i] = Long.parseLong(contributorIdsParam[i]);
			}
		}

		String[] columnIds = parameters.getListParameter("columnIds");

		Workbook xls = modelMgr.buildReport(start, intervalType, intervalCount,
				rootTaskId, taskDepth, onlyKeepTasksWithContributions,
				byContributor, contributorCentricMode, contributorIds,
				columnIds, false);
		xls.write(response.getOutputStream());
	}

}
