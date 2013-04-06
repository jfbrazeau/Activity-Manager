package org.activitymgr.ui.web.logic;

import java.util.Calendar;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IContributionsLogic extends ILogic<IContributionsLogic.View> {
	
	public interface View extends IView<IContributionsLogic> {

		void setDate(Calendar lastMonday);
		
	}

	void onPreviousYear();

	void onPreviousMonth();

	void onPreviousWeek();

	void onNextWeek();

	void onNextMonth();

	void onNextYear();

}
