package org.activitymgr.core.impl.dao;

import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dao.AbstractDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.dao.IReportDAO;
import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.dto.report.ReportItem;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ReportDAOImpl extends AbstractDAOImpl implements IReportDAO {

	/** Logger */
	private static Logger log = Logger.getLogger(ReportDAOImpl.class);
	
	@Inject
	private ITaskDAO taskDAO;
	
	@Inject
	private ICollaboratorDAO collaboratorDAO;
	

	@Override
	public Report buildReport(Calendar start, ReportIntervalType intervalType,
			int intervalCount, Task rootTask, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds,
			String[] orderContributorsBy) {		
		
		/*
		 * Retrieve contributors
		 */
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			/*
			 * Retrieve task tree
			 */
			String rootPath = rootTask != null ? rootTask.getFullPath() : "";
			int activityPathLength = taskDepth*2 + (rootPath != null ? rootPath.length() : 0);
			Map<Long, TaskSums> tasksByIdCache = new HashMap<Long, TaskSums>();
			Map<String, TaskSums> tasksByFullPathCache = new HashMap<String, TaskSums>();
			List<TaskSums> orderedTasks = new ArrayList<TaskSums>();
			// Register sub tasks
			if (taskDepth > 0) {
				StringWriter request = new StringWriter()
					.append("select ")
					.append("sum(leaftask.tsk_budget), sum(leaftask.tsk_initial_cons), sum(leaftask.tsk_todo), (count(leaftask.tsk_id)-1), ")
					.append(taskDAO.getColumnNamesRequestFragment("activitytask"))
					.append(" from TASK activitytask, TASK leaftask ")
					.append("where ")
					.append("(leaftask.tsk_id=activitytask.tsk_id or leaftask.tsk_path like concat(activitytask.tsk_path, activitytask.tsk_number, '%')) ");
				if (rootPath != null) {
					request.append("and activitytask.tsk_path like ? ");
				}
				request.append("and length(activitytask.tsk_path)<=? ")
					.append("group by activitytask.tsk_id ");
				request.append("order by ");
				// This helps to ensure parent tasks will be sorted before children tasks
				appendOrderByTaskPathFragment(request, "activitytask", taskDepth);
				pStmt = tx().prepareStatement(request.toString());
				int paramIdx = 1;
				if (rootPath != null) {
					pStmt.setString(paramIdx++, rootPath + "%");
				}
				
				pStmt.setInt(paramIdx++, activityPathLength-2);
				rs = pStmt.executeQuery();
				
				while (rs.next()) {
					TaskSums sums = new TaskSums();
					sums.setBudgetSum(rs.getLong(1));
					sums.setInitiallyConsumedSum(rs.getLong(2));
					sums.setTodoSum(rs.getLong(3));
					Task task = taskDAO.read(rs, 5);
					boolean isActivityTask = task.getFullPath().length() == activityPathLength;
					boolean hasNoChild = rs.getLong(4) == 0;
					// Task without any child or activity task are considered leaf
					sums.setLeaf(hasNoChild || isActivityTask);
					sums.setTask(task);
					tasksByIdCache.put(task.getId(), sums);
					tasksByFullPathCache.put(task.getFullPath(), sums);
					orderedTasks.add(sums);
					//System.out.println(sums.getTask().getFullPath() + " - " + sums.getTask().getName() + " - " + sums.getBudgetSum() + " - leaf : " + sums.isLeaf());
				}
				// Close the statement
				pStmt.close();
				pStmt = null;
			}
	
			/*
			 * Interval computation
			 */
			int startYear = start.get(Calendar.YEAR);
			int startMonth = start.get(Calendar.MONTH) + 1;
			int startDay = start.get(Calendar.DATE);
			int startDate = startYear*10000+startMonth*100+startDay;
			
			Calendar end = (Calendar) start.clone();
			end.add(intervalType.getIntType(), intervalCount);
			end.add(Calendar.DATE, -1);
			int endYear = end.get(Calendar.YEAR);
			int endMonth = end.get(Calendar.MONTH) + 1;
			int endDay = end.get(Calendar.DATE);
			int endDate = endYear*10000+endMonth*100+endDay;
		
			/*
			 * Retrieve contributions
			 */
			boolean byActivity = (taskDepth > 0);
			// Prepare the request
			StringWriter sw = new StringWriter();
			sw.append("select ");
			int collaboratorFieldsIndex = 1;
			// SELECT
			if (byActivity) {
				sw.append("activity.tsk_id, ");
				collaboratorFieldsIndex++;
			}
			switch (intervalType) {
			case WEEK:
			case DAY:
				sw.append("ctb_day, ");
				collaboratorFieldsIndex++;
			case MONTH:
				sw.append("ctb_month, ");
				collaboratorFieldsIndex++;
			case YEAR :
				sw.append("ctb_year, ");
				collaboratorFieldsIndex++;
			}
			sw.append("sum(ctb_duration)");
			collaboratorFieldsIndex++;
			// Append contributor if needed
			if (byContributor) {
				sw.append(", ");
				sw.append(collaboratorDAO.getColumnNamesRequestFragment(null));
			}
			// Append columns that are used in the order by clause not to let HSQLDB fail
			if (byActivity) {
				sw.append(", activity.tsk_path, activity.tsk_number");
			}
			
			sw.append("\nfrom TASK as ctbtask ");
			sw.append("\n\tleft join CONTRIBUTION on ctbtask.tsk_id = ctb_task ");
			if (byContributor) {
				sw.append("\n\tleft join COLLABORATOR on clb_id = ctb_contributor ");
			}
			if (byActivity) {
				sw.append("\n\tleft join TASK as activity on (ctbtask.tsk_id=activity.tsk_id or left(ctbtask.tsk_path, ?) = concat(activity.tsk_path, activity.tsk_number)) ");
			}

			// WHERE
			sw.append("\nwhere true ");
			// Filter 
			if (rootTask != null) {
				sw.append("and (ctbtask.tsk_id=? or left(ctbtask.tsk_path, ?) = ?) ");
			}
			if (contributorIds != null && contributorIds.length > 0) {
				sw.append("and ctb_contributor in (");
				for (int i=0; i<contributorIds.length; i++) {
					if (i > 0) {
						sw.append(", ");
					}
					sw.append("?");
				}
				sw.append(") ");
			}
			if (byActivity) {
				sw.append("and (");
				{
					// If the activity depth is equal to taskDepth (for contributions 
					// to deep tasks)
					sw.append("length(activity.tsk_path)=? ");
					sw.append("or ");
					// If the activity depth is < to parameter taskDepth
					// In such case, only leaf tasks must be used, container
					// tasks must be ignored (in the case of leaf tasks, we
					// have activity.id == contribution.task.id)
					sw.append("(ctbtask.tsk_id=activity.tsk_id and length(activity.tsk_path)<?) ");
				}
				sw.append(") ");
			}
			sw.append("and (ctb_year*10000+ctb_month*100+ctb_day) between ? and ?");
			// GROUP BY
			sw.append("\ngroup by ctb_year");
			if (byContributor) {
				sw.append(", clb_id");
			}
			if (intervalType != ReportIntervalType.YEAR) {
				sw.append(", ctb_month");
			}
			if (intervalType == ReportIntervalType.WEEK || intervalType == ReportIntervalType.DAY) {
				sw.append(", ctb_day");
			}
			if (taskDepth > 0) {
				sw.append(", activity.tsk_id");
			}
			
			// ORDER BY
			sw.append("\norder by ");
			// By default order collaborators by id
			String clbFragment = "clb_id, ";
			// But if possible order by given fields
			if (orderContributorsBy != null && orderContributorsBy.length > 0) {
				clbFragment = "";
				for (String orderContributorsByItem : orderContributorsBy) {
					clbFragment += collaboratorDAO.getColumnName(orderContributorsByItem) + ", ";
				}
			}
			if (byContributor) {
				if (byActivity){
					if (contributorCentricMode) {
						sw.append(clbFragment);
						appendOrderByTaskPathFragment(sw, "activity", taskDepth);
						sw.append(", ");
					}
					else {
						appendOrderByTaskPathFragment(sw, "activity", taskDepth);
						sw.append(", ");
						sw.append(clbFragment);
					}
				}
				else {
					sw.append(clbFragment);
				}
			}
			// If byContributor == false, orderByContributor can be ignored
			else if (byActivity) {
				appendOrderByTaskPathFragment(sw, "activity", taskDepth);
				sw.append(", ");
			}
			sw.append("ctb_year");
			if (intervalType != ReportIntervalType.YEAR) {
				sw.append(", ctb_month");
			}
			if (intervalType == ReportIntervalType.WEEK || intervalType == ReportIntervalType.DAY) {
				sw.append(", ctb_day");
			}
			
			String sql = sw.toString();
			//System.out.println(sql);
			
			// Build the request
			pStmt = tx().prepareStatement(sql);
			int idx = 1;
			if (byActivity) {
				pStmt.setInt(idx++, activityPathLength);
			}
			if (rootTask != null) {
				pStmt.setLong(idx++, rootTask.getId());
				pStmt.setInt(idx++, rootPath.length());
				pStmt.setString(idx++, rootPath);
			}
			if (contributorIds != null && contributorIds.length > 0) {
				for (Long contributorId : contributorIds) {
					pStmt.setLong(idx++, contributorId);
				}
			}
			if (byActivity) {
				pStmt.setInt(idx++, activityPathLength-2);
				pStmt.setInt(idx++, activityPathLength-2);
			}
			pStmt.setLong(idx++, startDate);
			pStmt.setLong(idx++, endDate);

			// Exécution de la requête
			int orderedTaskIndex = 0;
			rs = pStmt.executeQuery();
			Report report = new Report(start, intervalType, intervalCount, rootTask, taskDepth, byContributor, contributorCentricMode);
			ReportItem reportItem = null;
			Map<Long, Collaborator> collaboratorsMap = new HashMap<Long, Collaborator>();
			while (rs.next()) {
				Collaborator contributor = null;
				TaskSums contributedTask = null;
				idx = 1;
				if (byActivity) {
					long id = rs.getLong(idx++);
					contributedTask = tasksByIdCache.get(id);
				}
				if (byContributor) {
					long id = rs.getLong(collaboratorFieldsIndex);
					contributor = collaboratorsMap.get(id);
					if (contributor == null) {
						contributor = collaboratorDAO.read(rs, collaboratorFieldsIndex);
						collaboratorsMap.put(id, contributor);
					}
				}
				// See whether a new item must be created
				boolean newItem = false;
				if (reportItem == null) {
					newItem = true;
				} else {
					if (byActivity && !reportItem.getContributedTask().equals(contributedTask)) {
						newItem = true;
					}
					if (byContributor && !reportItem.getContributor().equals(contributor)) {
						newItem = true;
					}
				}
				if (newItem) {
					// If no task is present, simply create a report item
					if (!byActivity) {
						reportItem = new ReportItem(report, contributor);
					}
					else {
						// If in task centric mode (or without contributors which is equivalent), may have to insert rows without contributions
						// before adding new report line
						if (!onlyKeepTasksWithContributions && (!contributorCentricMode || !byContributor)) {
							if (!contributedTask.equals(orderedTasks.get(orderedTaskIndex))) {
								// If the last report item was about the same task, we must skeep the corresponding value
								// in the ordered task list
								if (reportItem != null && reportItem.getContributedTask().equals(orderedTasks.get(orderedTaskIndex))) {
									orderedTaskIndex++;
								}
								// Add empty rows (empty means without contributions, but these rows may be associated
								// to budgets for example).
								TaskSums cursor = null;
								while (!(cursor = orderedTasks.get(orderedTaskIndex)).equals(contributedTask)) {
									if (cursor.isLeaf()) {
										TaskSums[] tasks = buldTasksList(rootPath,
												tasksByFullPathCache, cursor.getTask().getFullPath());
										new ReportItem(report, null, tasks);
									}
									orderedTaskIndex++;
								}
							}
						}
						// Add report item
						TaskSums[] tasks = buldTasksList(rootPath,
								tasksByFullPathCache, contributedTask.getTask().getFullPath());
						reportItem = new ReportItem(report, contributor, tasks);
					}
				}
				
				// Read date
				int year = 0;
				int month = 1;
				int day = 1;
				switch (intervalType) {
				case WEEK:
				case DAY:
					day = rs.getInt(idx++);
				case MONTH:
					month = rs.getInt(idx++);
				case YEAR :
					year = rs.getInt(idx++);
				}
				
				// Compute index
				int intervalIdx = 0;
				switch (intervalType) {
				case WEEK:
				case DAY:
					Calendar date = Calendar.getInstance(start.getTimeZone());
					date.set(Calendar.DATE, day);
					date.set(Calendar.MONTH, month-1);
					date.set(Calendar.YEAR, year);
					intervalIdx = DateHelper.countDaysBetween(start, date);
					if (intervalType == ReportIntervalType.WEEK) {
						intervalIdx = intervalIdx/7;
					}
					break;
				case MONTH:
					intervalIdx = (year-startYear)*12 + month - startMonth;
					break;
				case YEAR :
					intervalIdx = year-startYear;
				}

				// Register contribution
				reportItem.addToContributionSum(intervalIdx, rs.getLong(idx++));
			}

			// Empty rows may have to be added at the end of the report (only in task centric mode or equivalent) 
			if (!onlyKeepTasksWithContributions && byActivity
					&& (!contributorCentricMode || !byContributor)) {
				while (++orderedTaskIndex < orderedTasks.size()) {
					TaskSums cursor = orderedTasks.get(orderedTaskIndex);
					if (cursor.isLeaf()) {
						TaskSums[] tasks = buldTasksList(rootPath,
								tasksByFullPathCache, cursor.getTask().getFullPath());
						new ReportItem(report, null, tasks);
					}
				}
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			return report;

		} catch (SQLException e) {
			log.info("Unexpected SQL error", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString("Unexpected SQL error"), e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	private void appendOrderByTaskPathFragment(StringWriter request,
			String alias, int taskDepth) {
		request.append("concat(").append(alias).append(".tsk_path, ").append(alias).append(".tsk_number, '");
		for (int i=0; i<taskDepth; i++) {
			request.append("00");
		}
		request.append("')");
	}

	private TaskSums[] buldTasksList(String rootPath,
			Map<String, TaskSums> tasksByFullPathCache, String fullpath) {
		int depth = (fullpath.length() - rootPath.length()) / 2;
		TaskSums[] tasks = new TaskSums[depth];
		for (int i=0 ; i<depth; i++) {
			tasks[i] = tasksByFullPathCache.get(fullpath.substring(0, (i+1)*2 + rootPath.length()));
		}
		return tasks;
	}

}
