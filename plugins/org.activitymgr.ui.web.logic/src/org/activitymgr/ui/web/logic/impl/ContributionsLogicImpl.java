package org.activitymgr.ui.web.logic.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelException;
import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.IContributionsLogic;

public class ContributionsLogicImpl extends AbstractLogicImpl<IContributionsLogic.View> implements IContributionsLogic {
	
	private Calendar date;
	private Duration[] durations;
	private TaskContributions[] weekContributions;

	public ContributionsLogicImpl(Context context, AbstractLogicImpl<?> parent) {
		super(context, parent);

		// Initialization
		date = new GregorianCalendar();
		try {
			durations = ModelMgr.getActiveDurations();
			String[] durationsStr = new String[durations.length];
			for (int i = 0; i < durations.length; i++)
				durationsStr[i] = StringHelper
						.hundredthToEntry(durations[i].getId());
			getView().setDurationLabels(durationsStr);
		}
		catch (DbException e) {
			throw new IllegalStateException("Unexpected error while retrieving durations", e);
		}
		
		// Fake change : add 0 year and update date in the view
		changeMondayAndUpdateView(Calendar.YEAR, 0);
		
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
	public void onDurationClicked(String taskCodePath, int dayOfWeek, int durationIdx) {
		try {
			TaskContributions tc = null;
			for (TaskContributions cursor : weekContributions) {
				if (cursor.getTaskCodePath().equals(taskCodePath)) {
					tc = cursor;
					break;
				}
			}
			Contribution contribution = tc.getContributions()[dayOfWeek];
			// First case : the contribution must be created
			if (contribution == null) {
				Duration duration = durations[durationIdx];
				contribution = new Contribution();
				contribution.setContributorId(getContext().getConnectedCollaborator().getId());
				contribution.setDurationId(duration.getId());
				contribution.setTaskId(tc.getTask().getId());
				Calendar clone = (Calendar) date.clone();
				clone.add(Calendar.DATE, dayOfWeek);
				contribution.setDate(clone);
				ModelMgr.createContribution(contribution, true);
				tc.getContributions()[dayOfWeek] = contribution;
				getView().updateDurationIndex(taskCodePath, dayOfWeek, durationIdx);
			}
			// Second case : the contribution must be updated
			else {
				Duration duration = durations[durationIdx];
				// If the duration that has been clicked is the one of the current contribution
				// it means that the contribution must be removed
				if (duration.getId() == contribution.getDurationId()) {
					ModelMgr.removeContribution(contribution, true);
					tc.getContributions()[dayOfWeek] = null;
					getView().updateDurationIndex(taskCodePath, dayOfWeek, -1);
				}
				// In other cases, an update is performed
				else {
					contribution.setDurationId(duration.getId());
					ModelMgr.updateContribution(contribution, true);
					getView().updateDurationIndex(taskCodePath, dayOfWeek, durationIdx);
				}
			}
		}
		catch (DbException e) {
			handleError(e);
		}
		catch (ModelException e) {
			handleError(e);
		}
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
		try {
			IntervalContributions intervalContributions = ModelMgr
					.getIntervalContributions(getContext().getConnectedCollaborator(),
							null, fromDate, toDate);
			weekContributions = intervalContributions.getTaskContributions();
		} catch (DbException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}
		// The result contains the contributions of the previous
		// week
		// We truncate it before proceeding.
		for (TaskContributions tc : weekContributions) {
			Contribution[] newContribs = new Contribution[7];
			System.arraycopy(tc.getContributions(), 7,
					newContribs, 0, 7);
			tc.setContributions(newContribs);
			int[] durationIndexes = new int[7];
			for (int i=0; i<7; i++) {
				Contribution c = tc.getContributions()[i];
				// The only thing that is known on the UI side about durations
				// is their index.
				durationIndexes[i] = c == null ? -1 : indexOfDuration(c.getDurationId());
			}
			getView().addWeekContribution(tc.getTaskCodePath(), tc.getTask().getName(), durationIndexes);
		}
	}

	private int indexOfDuration(long durationId) {
		int idx = -1;
		for (int i=0; i<durations.length; i++) {
			long duration = durations[i].getId();
			if (duration == durationId) {
				idx = i;
				break;
			}
		}
		return idx;
	}

	private static Calendar getMondayBefore(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}

}
