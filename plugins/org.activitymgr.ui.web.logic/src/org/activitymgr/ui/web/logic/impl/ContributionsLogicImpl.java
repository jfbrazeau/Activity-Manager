package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.ui.web.logic.IContributionsLogic;

public class ContributionsLogicImpl extends AbstractLogicImpl<IContributionsLogic.View> implements IContributionsLogic {
	
	private Calendar date;

	public ContributionsLogicImpl(Context context, AbstractLogicImpl<?> parent) {
		super(context, parent);
		
		date = new GregorianCalendar();
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

	private void changeMondayAndUpdateView(int amountType, int amount) {
		date.add(amountType, amount);
		date = getMondayBefore(date);
		getView().setDate(date);
	}

	private static Calendar getMondayBefore(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}

}
