package org.activitymgr.core.dto.report;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.util.StringHelper;

public class Report {
	
	private static final String NON_REPEATED_AMOUNT = "-.--";

	private final Collection<Calendar> dates = new ArrayList<Calendar>();
	
	private final ReportIntervalType intervalType;
	
	private final Task rootTask;
	
	private final int taskDepth;
	
	private final List<ReportItem> items = new ArrayList<ReportItem>();
	
	private final boolean byContributor;
	
	private final boolean isContributorCentric;

	private int intervalCount;

	public Report(Calendar start, ReportIntervalType intervalType, int intervalCount, Task rootTask, int taskDepth, boolean byContributor, boolean isContributorCentric) {
		this.intervalType = intervalType;
		this.rootTask = rootTask;
		this.taskDepth = taskDepth;
		this.byContributor = byContributor;
		this.isContributorCentric = isContributorCentric;
		this.intervalCount = intervalCount;
		Calendar date = (Calendar) start.clone();
		for (int i=0; i<intervalCount; i++) {
			Calendar d = (Calendar) date.clone();
			dates.add(d);
			date.add(intervalType.getIntType(), 1);
		}
	}
	
	public int getIntervalCount() {
		return intervalCount;
	}
	
	public Task getRootTask() {
		return rootTask;
	}

	public ReportIntervalType getIntervalType() {
		return intervalType;
	}
	
	public List<ReportItem> getItems() {
		return items;
	}
	
	public int getTaskDepth() {
		return taskDepth;
	}

	public boolean isContributorCentric() {
		return isContributorCentric;
	}

	protected void add(ReportItem item) {
		items.add(item);
	}

	public Collection<Calendar> getDates() {
		return dates;
	}

	private static final int TASK_PATH_PAD = 15;
	private static final int TASK_NAME_PAD = 15;
	private static final int BUDGET_NAME_PAD = 7;
	private static final int INITIALLY_CONSUMMED_NAME_PAD = 7;
	private static final int ETC_NAME_PAD = 7;
	private static final int COLLABORATOR_PAD = 10;
	private static final int CONTRIBUTION_PAD = 7;

	@Override
	public String toString() {
		String header = header();
		String delim = turnInDelimiter(header);
		
		StringWriter sw = new StringWriter();
		sw.append(delim);
		sw.append(header.toString());
		sw.append(delim);
		
		long[] sums = new long[intervalCount];
		long budgetSum = 0;
		long initiallyConsumedSum = 0;
		long etcSum = 0;
		ReportItem lastItem = null;
		for (ReportItem item : items) {
			startRow(sw);
			TaskSums contributedTask = item.getContributedTask();
			Collaborator contributor = item.getContributor();
			if (byContributor && isContributorCentric) {
				appendCellRight(sw, contributor != null ? contributor.getLogin() : "", COLLABORATOR_PAD);
			}
			if (taskDepth > 0) {
				StringWriter pathSw = new StringWriter();
				for (TaskSums task : item.getTasks()) {
					pathSw.append('/');
					pathSw.append(task.getTask().getCode());
				}
				appendCellRight(sw, pathSw.toString(), TASK_PATH_PAD);
				appendCellRight(sw, contributedTask.getTask().getName(), TASK_NAME_PAD);
			}
			if (byContributor && !isContributorCentric) {
				appendCellRight(sw, contributor != null ? contributor.getLogin() : "", COLLABORATOR_PAD);
			}
			if (taskDepth > 0 && !isContributorCentric) {
				if (lastItem == null || !lastItem.getContributedTask().equals(item.getContributedTask())) {
					budgetSum += contributedTask.getBudgetSum();
					appendCellLeft(sw, StringHelper.hundredthToEntry(contributedTask.getBudgetSum()), BUDGET_NAME_PAD);
					initiallyConsumedSum += contributedTask.getInitiallyConsumedSum();
					appendCellLeft(sw, StringHelper.hundredthToEntry(contributedTask.getInitiallyConsumedSum()), INITIALLY_CONSUMMED_NAME_PAD);
				}
				else {
					appendCellLeft(sw, NON_REPEATED_AMOUNT, BUDGET_NAME_PAD);
					appendCellLeft(sw, NON_REPEATED_AMOUNT, INITIALLY_CONSUMMED_NAME_PAD);
				}
			}
			for (int i=0; i<dates.size(); i++) {
				sums[i] += item.getContributionSum(i);
				appendCellLeft(sw, StringHelper.hundredthToEntry(item.getContributionSum(i)), CONTRIBUTION_PAD);
			}
			if (taskDepth > 0 && !isContributorCentric) {
				if (lastItem == null || !lastItem.getContributedTask().equals(item.getContributedTask())) {
					etcSum += contributedTask.getTodoSum();
					appendCellLeft(sw, StringHelper.hundredthToEntry(contributedTask.getTodoSum()), ETC_NAME_PAD);
				}
				else {
					appendCellLeft(sw, NON_REPEATED_AMOUNT, ETC_NAME_PAD);
				}
			}
			endRow(sw);
			lastItem = item;
		}
		sw.append(delim);

		// Totals
		startRow(sw);
		if (byContributor && isContributorCentric) {
			appendCellRight(sw, "", COLLABORATOR_PAD);
		}
		if (taskDepth > 0) {
			appendCellRight(sw, "", TASK_PATH_PAD);
			appendCellRight(sw, "", TASK_NAME_PAD);
		}
		if (byContributor && !isContributorCentric) {
			appendCellRight(sw, "", COLLABORATOR_PAD);
		}
		if (taskDepth > 0 && !isContributorCentric) {
			appendCellLeft(sw, StringHelper.hundredthToEntry(budgetSum), BUDGET_NAME_PAD);
			appendCellLeft(sw, StringHelper.hundredthToEntry(initiallyConsumedSum), INITIALLY_CONSUMMED_NAME_PAD);
		}
		for (int i=0; i<dates.size(); i++) {
			appendCellLeft(sw, StringHelper.hundredthToEntry(sums[i]), CONTRIBUTION_PAD);
		}
		if (taskDepth > 0 && !isContributorCentric) {
			appendCellLeft(sw, StringHelper.hundredthToEntry(etcSum), ETC_NAME_PAD);
		}
		endRow(sw);
		sw.append(delim);
		
		return sw.toString();
	}

	private String turnInDelimiter(String header) {
		StringWriter delimSw = new StringWriter();
		for (char car : header.toString().toCharArray()) {
			switch (car) {
			case '|' :
				delimSw.append('+');
				break;
			case '\n' :
				delimSw.append('\n');
				break;
			default :
				delimSw.append('-');
			}
		}
		return delimSw.toString();
	}

	private String header() {
		StringWriter sw = new StringWriter();
		startRow(sw);
		if (byContributor && isContributorCentric) {
			appendCellRight(sw, "Contributor", COLLABORATOR_PAD);
		}
		if (taskDepth > 0) {
			appendCellRight(sw, "Path", TASK_PATH_PAD);
			appendCellRight(sw, "Name", TASK_NAME_PAD);
		}
		if (byContributor && !isContributorCentric) {
			appendCellRight(sw, "Contributor", COLLABORATOR_PAD);
		}
		if (taskDepth > 0 && !isContributorCentric) {
			appendCellLeft(sw, "Budget", BUDGET_NAME_PAD);
			appendCellLeft(sw, "Ini. Cons.", INITIALLY_CONSUMMED_NAME_PAD);
		}
		String format = null;
		switch (intervalType) {
		case DAY:
			format = "ddMMyy";
			break;
		case WEEK:
			format = "'S'w-yy";
			break;
		case MONTH :
			format = "MMyy";
			break;
		case YEAR :
			format = "yyyy";
			break;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		for (Calendar date : dates) {
			appendCellLeft(sw, sdf.format(date.getTime()), CONTRIBUTION_PAD);
		}
		if (taskDepth > 0 && !isContributorCentric) {
			appendCellLeft(sw, "ETC", ETC_NAME_PAD);
		}
		endRow(sw);
		return sw.toString();
	}

	private StringWriter startRow(StringWriter sw) {
		return sw.append('|');
	}

	private StringWriter endRow(StringWriter sw) {
		return sw.append('\n');
	}
	
	private void appendCellLeft(StringWriter sw, String s, int pad) {
		appendCell(sw, s, ' ', pad, false);
	}

	private void appendCellRight(StringWriter sw, String s, int pad) {
		appendCell(sw, s, ' ', pad, true);
	}

	private void appendCell(StringWriter sw, String s, char pad, int padCount, boolean alignRight) {
		if (s == null) {
			s = "";
		}
		if (s.length() == padCount) {
			sw.append(s);
		}
		else if (s.length() < padCount) {
			if (alignRight) {
				sw.append(s);
			}
			for (int i = s.length(); i < padCount; i++) {
				sw.append(pad);
			}
			if (!alignRight) {
				sw.append(s);
			}
		}
		else {
			sw.append(s.substring(0,  padCount - 1));
			sw.append(".");
		}
		startRow(sw);
	}

}
