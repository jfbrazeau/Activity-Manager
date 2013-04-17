package org.activitymgr.ui.web.logic;

import java.util.Calendar;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IContributionsLogic extends ILogic<IContributionsLogic.View> {
	
	public interface View extends IView<IContributionsLogic> {

		void setDate(Calendar lastMonday);

		void setDayLabels(String[] dayLabels);

		void addWeekContribution(String taskCodePath, String name,
				String[] durations);

		void updateDuration(String taskCodePath, int dayOfWeek,
				String duration);

		void removeAllWeekContributions();

		void setDayTotal(int dayOfWeek, String total);

		void setTotal(String total);

		void setTaskTotal(String taskCodePath, String total);

		void focusOnCell(String taskCodePath, int dayOfWeek);

		// TODO externalize ?
		void setTaskWeekPrevision(String taskCodePath, String previsionalWeekDuration);
		
	}

	void onPreviousYear();

	void onPreviousMonth();

	void onPreviousWeek();

	void onNextWeek();

	void onNextMonth();

	void onNextYear();

	void onDurationChanged(String taskCodePath, int dayOfWeek, String duration);

	void onDateChange(Calendar value);

	void onTaskButtonClicked();

}
