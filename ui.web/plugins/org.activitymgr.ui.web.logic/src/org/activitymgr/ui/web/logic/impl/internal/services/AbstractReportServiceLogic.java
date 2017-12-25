package org.activitymgr.ui.web.logic.impl.internal.services;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.impl.AbstractServiceWithAuthenticationLogic;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.inject.Inject;

public abstract class AbstractReportServiceLogic extends
		AbstractServiceWithAuthenticationLogic {

	public static final String COLUMN_IDS_PARAMETER = "columnIds";
	public static final String CONTRIBUTOR_IDS_PARAMETERS = "contributorIds";
	public static final String CONTRIBUTOR_CENTRIC_MODE_PARAMETER = "contributorCentricMode";
	public static final String BY_CONTRIBUTOR_PARAMETER = "byContributor";
	public static final String ONLY_KEEP_TASKS_WITH_CONTRIBUTIONS_PARAMETER = "onlyKeepTasksWithContributions";
	public static final String TASK_DEPTH_PARAMETER = "taskDepth";
	public static final String ROOT_TASK_PARAMETER = "rootTask";
	public static final String INTERVAL_COUNT_PARAMETER = "intervalCount";
	public static final String INTERVAL_TYPE_PARAMETER = "intervalType";
	public static final String START_PARAMETER = "start";

	@Inject
	private IModelMgr modelMgr;

	@Override
	protected final void doService(Collaborator connected, Request parameters,
			Response response)
			throws ModelException, IOException {

		Calendar start = null;
		String startParam = parameters.getParameter(START_PARAMETER);
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
		String intervalTypeParam = parameters.getParameter(INTERVAL_TYPE_PARAMETER);
		if (intervalTypeParam != null) {
			intervalType = ReportIntervalType.valueOf(intervalTypeParam);
		}

		Integer intervalCount = null;
		String intervalCountParam = parameters.getParameter(INTERVAL_COUNT_PARAMETER);
		if (intervalCountParam != null) {
			intervalCount = Integer.parseInt(intervalCountParam);
		}

		Long rootTaskId = null;
		String rootTaskParam = parameters.getParameter(ROOT_TASK_PARAMETER);
		if (rootTaskParam != null && !"".equals(rootTaskParam)) {
			Task rootTask = modelMgr.getTaskByCodePath(rootTaskParam);
			if (rootTask != null) {
				rootTaskId = rootTask.getId();
			}
		}

		int taskDepth = 1;
		String taskDepthParam = parameters.getParameter(TASK_DEPTH_PARAMETER);
		if (taskDepthParam != null) {
			taskDepth = Integer.parseInt(taskDepthParam);
		}

		boolean onlyKeepTasksWithContributions = !"false".equals(parameters
				.getParameter(ONLY_KEEP_TASKS_WITH_CONTRIBUTIONS_PARAMETER));

		String byContributorParam = parameters
				.getParameter(BY_CONTRIBUTOR_PARAMETER);
		boolean byContributor = byContributorParam == null
				|| "true".equals(byContributorParam);

		boolean contributorCentricMode = "true".equals(parameters
				.getParameter(CONTRIBUTOR_CENTRIC_MODE_PARAMETER));

		long[] contributorIds = null;
		String contributorIdsParam = parameters
				.getParameter(CONTRIBUTOR_IDS_PARAMETERS);
		if (!"*".equals(contributorIdsParam)) {
			if (contributorIdsParam != null) {
				String[] values = contributorIdsParam.split(",");
				contributorIds = new long[values.length];
				for (int i = 0; i < values.length; i++) {
					contributorIds[i] = Long.parseLong(values[i]);
				}
			} else {
				contributorIds = new long[] { connected.getId() };
			}
		}

		String[] columnIds = parameters.getListParameter(COLUMN_IDS_PARAMETER);
		if (columnIds == null) {
			columnIds = new String[] { "task.path", "task.name" };
		}

		Workbook xls = modelMgr.buildReport(start, intervalType, intervalCount,
				rootTaskId, taskDepth, onlyKeepTasksWithContributions,
				byContributor, contributorCentricMode, contributorIds,
				columnIds, false);
		doService(connected, parameters, response, xls);
	}

	protected abstract void doService(Collaborator connected, Request request,
			Response response, Workbook report) throws IOException;

}
