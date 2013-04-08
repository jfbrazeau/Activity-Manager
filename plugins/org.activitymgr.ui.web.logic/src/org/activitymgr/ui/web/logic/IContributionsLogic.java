package org.activitymgr.ui.web.logic;

import java.util.Calendar;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IContributionsLogic extends ILogic<IContributionsLogic.View> {
	
	public interface View extends IView<IContributionsLogic> {

		void setDate(Calendar lastMonday);

		void setDurationLabels(String[] durationsStr);

		void setDayLabels(String[] dayLabels);

		void addWeekContribution(String taskCodePath, String name,
				int[] durationIndexes);

		void updateDurationIndex(String taskCodePath, int dayOfWeek,
				int durationIdx);

		void removeAllWeekContributions();

		void setDayTotal(int dayOfWeek, String total);

		void setTotal(String total);

		void setTaskTotal(String taskCodePath, String total);
		
	}

	void onPreviousYear();

	void onPreviousMonth();

	void onPreviousWeek();

	void onNextWeek();

	void onNextMonth();

	void onNextYear();

	void onDurationClicked(String taskCodePath, int dayOfWeek, int durationIdx);

	void onDateChange(Calendar value);

}
