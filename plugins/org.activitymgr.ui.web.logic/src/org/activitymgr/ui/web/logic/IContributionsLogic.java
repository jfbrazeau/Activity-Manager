package org.activitymgr.ui.web.logic;

import java.util.Calendar;
import java.util.List;

// TODO clear
public interface IContributionsLogic extends ILogic<IContributionsLogic.View> {
	
	void onPreviousYear();

	void onPreviousMonth();

	void onPreviousWeek();

	void onNextWeek();

	void onNextMonth();

	void onNextYear();

	void onDateChange(Calendar value);

	public interface View extends ILogic.IView<IContributionsLogic> {
		
		void setColumnIdentifiers(List<String> ids);
		
		void setDate(Calendar lastMonday);

		void removeAllWeekContributions();

		void addWeekContribution(long taskId, List<ILogic.IView<?>> cellViews);
		
		void setColumnFooter(String id, String value);
		
		void addAction(IActionLogic.View actionView);

	}

}
