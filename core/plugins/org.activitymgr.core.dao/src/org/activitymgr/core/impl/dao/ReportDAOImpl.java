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
	public Report buildReport(Calendar start, ReportIntervalType intervalType, int intervalCount, Long rootTaskId, int taskDepth,
			boolean byContributor, boolean orderByContributor) {
		if (taskDepth < 0) {
			taskDepth = 0;
		}
		
		// 1. Récup arbo taches (pour les parents) avec la profondeur qui va bien
		// 2. Récup collaborateurs impliqués sur la période
		// 3. Récup et cumul des contributions pour les taches au moins aussi profonde que la profondeur spécifiée
		// 4. Récup des contributions pour les taches moins profondes
		
		/*
		 * Retrieve task tree
		 */
		Task rootTask = rootTaskId != null ? taskDAO.selectByPK(rootTaskId) : null;
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
		switch (intervalType) {
		case YEAR :
			start.set(Calendar.MONTH, 0);
		case MONTH:
			start.set(Calendar.DATE, 1);
		case WEEK:
		case DAY:
		}

		start.set(Calendar.HOUR_OF_DAY, 12);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		int startYear = start.get(Calendar.YEAR);
		int startMonth = start.get(Calendar.MONTH) + 1;
		int startDay = start.get(Calendar.DATE);
		int startDate = startYear*10000+startMonth*100+startDay;
		
		Calendar end = (Calendar) start.clone();
		end.add(intervalType.getIntType(), intervalCount - 1);
		if (!ReportIntervalType.DAY.equals(intervalType)) {
			end.add(Calendar.DATE, -1);
		}
		int endYear = end.get(Calendar.YEAR);
		int endMonth = end.get(Calendar.MONTH) + 1;
		int endDay = end.get(Calendar.DATE);
		int endDate = endYear*10000+endMonth*100+endDay;
		//System.out.println("Start : " + startDate + ", end:" + endDate);
		
		/*
		 * Retrieve involved collaborators
		 */
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			StringWriter sw = new StringWriter();
			sw.append("select distinct ");
			sw.append(collaboratorDAO.getColumnNamesRequestFragment(null));
			sw.append(" from CONTRIBUTION ");
			sw.append(" left join COLLABORATOR on ctb_contributor = clb_id");
			sw.append(" where (ctb_year*10000+ctb_month*100+ctb_day) between ? and ?");
			
			pStmt = tx().prepareStatement(sw.toString());
			pStmt.setLong(1, startDate);
			pStmt.setLong(2, endDate);

			// Exécution de la requête
			Map<Long, Collaborator> collaboratorsMap = new HashMap<Long, Collaborator>();
			rs = pStmt.executeQuery();
			while (rs.next()) {
				Collaborator c = collaboratorDAO.read(rs, 1);
				collaboratorsMap.put(c.getId(), c);
			}			
			
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
	
			/*
			 * Retrieve contributions
			 */
			boolean byActivity = (taskDepth > 0);
			// Prepare the request
			sw = new StringWriter();
			sw.append("select ");
			// SELECT
			if (byActivity) {
				sw.append("activity.tsk_id, ");
			}
			if (byContributor) {
				sw.append("clb_id, ");
			}
			switch (intervalType) {
			case WEEK:
			case DAY:
				sw.append("ctb_day, ");
			case MONTH:
				sw.append("ctb_month, ");
			case YEAR :
				sw.append("ctb_year, ");
			}
			sw.append("sum(ctb_duration)");
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
			if (rootTaskId != null) {
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
				sw.append(", activity.tsk_path, activity.tsk_number");
			}
			
			// ORDER BY
			sw.append("\norder by ");
			String activityFragment = "activity.tsk_path, activity.tsk_number, ";
			String clbFragment = "clb_login, ";
			if (byContributor) {
				if (byActivity){
					if (orderByContributor) {
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
			//System.out.println(sql);
			
			// Build the request
			pStmt = tx().prepareStatement(sql);
			int idx = 1;
			int activityPathLength = taskDepth*2 + (rootTask != null ? rootTask.getFullPath().length() : 0);
			if (byActivity) {
				pStmt.setInt(idx++, activityPathLength);
			}
			if (rootTaskId != null) {
				pStmt.setLong(idx++, rootTaskId);
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
			Report report = new Report(start, intervalType, intervalCount, rootTask, taskDepth, byContributor, orderByContributor);
			ReportItem reportItem = null;
			while (rs.next()) {
				Collaborator contributor = null;
				Task contributedTask = null;
				idx = 1;
				if (byActivity) {
					long id = rs.getLong(idx++);
					contributedTask = tasksByIdCache.get(id);
				}
				if (byContributor) {
					long id = rs.getLong(idx++);
					contributor = collaboratorsMap.get(id);
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
					throw new IllegalStateException("Not implemented yet");
				case DAY:
					Calendar date = Calendar.getInstance(start.getTimeZone());
					date.set(Calendar.DATE, day);
					date.set(Calendar.MONTH, month-1);
					date.set(Calendar.YEAR, year);
					intervalIdx = DateHelper.countDaysBetween(start, date);
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
