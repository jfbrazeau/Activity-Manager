package org.activitymgr.core.impl.dao;

import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.activitymgr.core.dao.AbstractDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dao.IReportDAO;
import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.dto.report.ReportItem;
import org.activitymgr.core.orm.query.LikeStatement;
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
	public Report buildReport(Calendar start, ReportIntervalType intervalType, int intervalCount, Task rootTask, int taskDepth,
			boolean byContributor, boolean contributorCentered, String[] orderContributorsBy) {
		/*
		 * Retrieve task tree
		 */
		String rootPath = rootTask != null ? rootTask.getFullPath() : "";
		Map<Long, Task> tasksByIdCache = new HashMap<Long, Task>();
		Map<String, Task> tasksByFullpathCache = new HashMap<String, Task>();
		// Register root task
		if (rootTask != null) {
			tasksByIdCache.put(rootTask.getId(), rootTask);
			tasksByFullpathCache.put(rootTask.getFullPath(), rootTask);
		}
		// Register sub tasks
		if (taskDepth > 0) {
			Task[] tasks = taskDAO.select(new String[] { "path" }, new Object[] { new LikeStatement(rootPath + "%") }, new Object[] { "path", "number" }, -1);
			for (Task task : tasks) {
				tasksByIdCache.put(task.getId(), task);
				tasksByFullpathCache.put(task.getFullPath(), task);
			}
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
		 * Retrieve contributors
		 */
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
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
			String activityFragment = "activity.tsk_path, activity.tsk_number, ";
			// By default order collaborators by id
			String clbFragment = "clb_login, ";
			// But if possible order by given fields
			if (orderContributorsBy != null && orderContributorsBy.length > 0) {
				clbFragment = "";
				for (String orderContributorsByItem : orderContributorsBy) {
					clbFragment += collaboratorDAO.getColumnName(orderContributorsByItem) + ", ";
				}
			}
			if (byContributor) {
				if (byActivity){
					if (contributorCentered) {
						sw.append(clbFragment);
						sw.append(activityFragment);
					}
					else {
						sw.append(activityFragment);
						sw.append(clbFragment);
					}
				}
				else {
					sw.append(clbFragment);
				}
			}
			// If byContributor == false, orderByContributor can be ignored
			else if (byActivity) {
				sw.append(activityFragment);
			}
			sw.append("ctb_year");
			if (intervalType != ReportIntervalType.YEAR) {
				sw.append(", ctb_month");
			}
			if (intervalType == ReportIntervalType.WEEK || intervalType == ReportIntervalType.DAY) {
				sw.append(", ctb_day");
			}
			
			String sql = sw.toString();
			System.out.println(sql);
			
			// Build the request
			pStmt = tx().prepareStatement(sql);
			int idx = 1;
			int activityPathLength = taskDepth*2 + (rootTask != null ? rootTask.getFullPath().length() : 0);
			if (byActivity) {
				pStmt.setInt(idx++, activityPathLength);
			}
			if (rootTask != null) {
				pStmt.setLong(idx++, rootTask.getId());
				pStmt.setInt(idx++, rootPath.length());
				pStmt.setString(idx++, rootPath);
			}
			if (byActivity) {
				pStmt.setInt(idx++, activityPathLength-2);
				pStmt.setInt(idx++, activityPathLength-2);
			}
			pStmt.setLong(idx++, startDate);
			pStmt.setLong(idx++, endDate);

			// Exécution de la requête
			rs = pStmt.executeQuery();
			Report report = new Report(start, intervalType, intervalCount, rootTask, taskDepth, byContributor, contributorCentered);
			ReportItem reportItem = null;
			Map<Long, Collaborator> collaboratorsMap = new HashMap<Long, Collaborator>();
			while (rs.next()) {
				Collaborator contributor = null;
				Task contributedTask = null;
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
					if (reportItem.getContributedTask() != null && !reportItem.getContributedTask().equals(contributedTask)) {
						newItem = true;
					}
					if (reportItem.getContributor() != null && !reportItem.getContributor().equals(contributor)) {
						newItem = true;
					}
				}
				if (newItem) {
					if (byActivity) {
						String fullpath = contributedTask.getFullPath();
						int depth = (fullpath.length() - rootPath.length()) / 2;
						Task[] tasks = new Task[depth];
						for (int i=0 ; i<depth; i++) {
							tasks[i] = tasksByFullpathCache.get(fullpath.substring(0, (i+1)*2 + rootPath.length()));
						}
						reportItem = new ReportItem(report, contributor, tasks);
					}
					else {
						reportItem = new ReportItem(report, contributor);
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

}
