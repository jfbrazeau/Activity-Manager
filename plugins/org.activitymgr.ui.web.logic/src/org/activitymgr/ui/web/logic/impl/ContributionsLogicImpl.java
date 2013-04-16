package org.activitymgr.ui.web.logic.impl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelException;
import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class ContributionsLogicImpl extends AbstractLogicImpl<IContributionsLogic.View> implements IContributionsLogic {
	
	private Calendar date;
	private List<TaskContributions> weekContributions = new ArrayList<TaskContributions>();
	private HSSFSheet prevSheet;

	public ContributionsLogicImpl(Context context, AbstractLogicImpl<?> parent) {
		super(context, parent);

		// TODO externalize ?
		try {
			HSSFWorkbook wb = new HSSFWorkbook(ContributionsLogicImpl.class.getClassLoader().getResourceAsStream("/prev.xls"));
			prevSheet = wb.getSheetAt(0);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		// Initialization
		date = new GregorianCalendar();
		// Fake change : add 0 year and update date in the view
		changeMondayAndUpdateView(Calendar.YEAR, 0);
		
	}

	private Map<String, Long> collectPrev() {
		Map<String, Long> result = new HashMap<String, Long>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		long expectedMonday = Long.parseLong(sdf.format(date.getTime()));
		System.out.println("Collectiong for " + expectedMonday);
		String expectedUserId = getContext().getConnectedCollaborator().getLogin();
		Iterator<?> it = prevSheet.rowIterator();
		it.next(); // First line ignored
		while (it.hasNext()) {
			HSSFRow row = (HSSFRow) it.next();
			String user = row.getCell((short) 0).getStringCellValue();
			System.out.println("user = " + user);
			if (!expectedUserId.equals(user)) {
				System.out.println("ignoring line");
			}
			else {
				long monday = (long) row.getCell((short) 1).getNumericCellValue();
				System.out.println("monday = " + monday);
				if (expectedMonday != monday) {
					System.out.println("ignoring line");
				}
				else {
					String taskPath = row.getCell((short) 2).getStringCellValue();
					System.out.println("taskPath = " + taskPath);
					result.put(taskPath, (long) (row.getCell((short) 3).getNumericCellValue() * 100));
				}
			}
		}
		return result;
	}
	
	@Override
	public void onPreviousYear() {
		changeMondayAndUpdateView(Calendar.YEAR, -1);
	}

	@Override
	public void onPreviousMonth() {
		changeMondayAndUpdateView(Calendar.MONTH, -1);
	}

	@Override
	public void onPreviousWeek() {
		changeMondayAndUpdateView(Calendar.WEEK_OF_YEAR, -1);
	}

	@Override
	public void onNextWeek() {
		changeMondayAndUpdateView(Calendar.WEEK_OF_YEAR, 1);
	}

	@Override
	public void onNextMonth() {
		changeMondayAndUpdateView(Calendar.MONTH, 1);
	}

	@Override
	public void onNextYear() {
		changeMondayAndUpdateView(Calendar.YEAR, 1);
	}

	@Override
	public void onDurationChanged(String taskCodePath, int dayOfWeek, String duration) {
		System.out.println("onDurationChanged(" + taskCodePath + ", " + dayOfWeek + ", " + duration + ")");
		try {
			long durationId = 0;
			if (duration != null && !"".equals(duration.trim())) {
				durationId = StringHelper.entryToHundredth(duration.replaceAll(",", "."));
			}

			TaskContributions tc = getTaskContributions(taskCodePath);
			Contribution contribution = tc.getContributions()[dayOfWeek];
			// First case : the contribution must be created
			if (contribution == null) {
				if (durationId != 0) {
					contribution = new Contribution();
					contribution.setContributorId(getContext().getConnectedCollaborator().getId());
					contribution.setDurationId(durationId);
					contribution.setTaskId(tc.getTask().getId());
					Calendar clone = (Calendar) date.clone();
					clone.add(Calendar.DATE, dayOfWeek);
					contribution.setDate(clone);
					ModelMgr.createContribution(contribution, true);
					tc.getContributions()[dayOfWeek] = contribution;
				}
			}
			// Second case : the contribution must be removed
			else if (durationId == 0) {
				ModelMgr.removeContribution(contribution, true);
				tc.getContributions()[dayOfWeek] = null;
			}
			// Third case : the contribution must be updated
			else {
				contribution.setDurationId(durationId);
				ModelMgr.updateContribution(contribution, true);
			}
			
			// Update the view
			duration = durationId == 0 ? "" : StringHelper.hundredthToEntry(durationId);
			getView().updateDuration(taskCodePath, dayOfWeek, duration);

			// Update totals
			updateTotals();
		}
		catch (DbException e) {
			handleError(e);
		}
		catch (ModelException e) {
			getRoot().getView().showNotification(e.getMessage());
			getView().focusOnCell(taskCodePath, dayOfWeek);
		}
		catch (StringFormatException e) {
			getRoot().getView().showNotification(e.getMessage());
			getView().focusOnCell(taskCodePath, dayOfWeek);
		}
	}

	@Override
	public void onDateChange(Calendar value) {
		date = value;
		changeMondayAndUpdateView(Calendar.DATE, 0);
	}

	private void updateTotals() {
		long total = 0;
		for (int dayOfWeek=0; dayOfWeek<7; dayOfWeek++) {
			long dayTotal = 0;
			for (TaskContributions tc : weekContributions) {
				Contribution c = tc.getContributions()[dayOfWeek];
				if (c != null) {
					dayTotal += c.getDurationId();
					total += c.getDurationId();
				}
			}
			getView().setDayTotal(dayOfWeek, StringHelper
					.hundredthToEntry(dayTotal));
		}
		getView().setTotal(StringHelper
				.hundredthToEntry(total));
		for (TaskContributions tc : weekContributions) {
			long taskTotal = 0;
			for (int dayOfWeek=0; dayOfWeek<7; dayOfWeek++) {
				Contribution c = tc.getContributions()[dayOfWeek];
				if (c != null) {
					taskTotal += c.getDurationId();
				}
			}
			getView().setTaskTotal(tc.getTaskCodePath(), StringHelper
					.hundredthToEntry(taskTotal));
		}
		
	}

	private TaskContributions getTaskContributions(String taskCodePath) {
		for (TaskContributions cursor : weekContributions) {
			if (cursor.getTaskCodePath().equals(taskCodePath)) {
				return cursor;
			}
		}
		return null;
	}

	private void changeMondayAndUpdateView(int amountType, int amount) {
		// Update date
		date.add(amountType, amount);
		date = getMondayBefore(date);
		getView().setDate(date);

		// Update day labels
		String[] dayLabels = new String[7];
		Calendar cursor = (Calendar) date.clone();
		// TODO manage locale format (MM/dd in england)
		SimpleDateFormat sdf = new SimpleDateFormat("E dd/MM");
		for (int i = 0; i < 7; i++) {
			dayLabels[i] = sdf.format(cursor.getTime());
			cursor.add(Calendar.DATE, 1);
		}
		getView().setDayLabels(dayLabels);
		
		// Recherche des taches déclarées pour cet utilisateur
		// pour la semaine courante (et la semaine passée pour
		// réafficher automatiquement les taches de la semaine
		// passée)
		Calendar fromDate = (Calendar) date.clone();
		fromDate.add(Calendar.DATE, -7);
		Calendar toDate = (Calendar) date.clone();
		toDate.add(Calendar.DATE, 6);
		weekContributions.clear();
		try {
			IntervalContributions intervalContributions = ModelMgr
					.getIntervalContributions(getContext().getConnectedCollaborator(),
							null, fromDate, toDate);
			TaskContributions[] weekContributionsArray = intervalContributions.getTaskContributions();
			weekContributions.addAll(Arrays.asList(weekContributionsArray));
			
		} catch (DbException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}

		// TODO externalize
		// Inject previsional tasks
		Map<String, Long> prev = collectPrev();
		try {
			for (String taskPath : prev.keySet()) {
				if (getTaskContributions(taskPath) == null) {
					TaskContributions tc = new TaskContributions();
					tc.setTaskCodePath(taskPath);
					tc.setTask(ModelMgr.getTaskByCodePath(taskPath));
					tc.setContributions(new Contribution[14]);
					weekContributions.add(tc);
				}
			}
		} catch (DbException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}
		
		// The result contains the contributions of the previous
		// week
		// We truncate it before proceeding.
		getView().removeAllWeekContributions();
		// TODO comparator as constant
		Collections.sort(weekContributions, new Comparator<TaskContributions>() {
			@Override
			public int compare(TaskContributions tc1, TaskContributions tc2) {
				return tc1.getTask().getFullPath().compareTo(tc2.getTask().getFullPath());
			}
		});
		for (TaskContributions tc : weekContributions) {
			Contribution[] newContribs = new Contribution[7];
			System.arraycopy(tc.getContributions(), 7,
					newContribs, 0, 7);
			tc.setContributions(newContribs);
			String[] durations = new String[7];
			for (int i=0; i<7; i++) {
				Contribution c = tc.getContributions()[i];
				// The only thing that is known on the UI side about durations
				// is their index.
				durations[i] = c == null ? "" : StringHelper.hundredthToEntry(c.getDurationId());
			}
			getView().addWeekContribution(tc.getTaskCodePath(), tc.getTask().getName(), durations);

		}
		
		// TODO externalize : Update prev
		for (String taskPath : prev.keySet()) {
			getView().setTaskWeekPrevision(taskPath, StringHelper.hundredthToEntry(prev.get(taskPath)));
		}
		
		// Update totals
		updateTotals();
	}

	private static Calendar getMondayBefore(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}

}
