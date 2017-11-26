package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class ReportsTabLogicImpl extends AbstractTabLogicImpl<IReportsTabLogic.View> implements IReportsTabLogic {

	static enum ReportIntervalBoundsMode {
		AUTOMATIC, LOWER_BOUND, BOTH_BOUNDS
	}
	
	@Inject(optional = true)
	private Set<ITabButtonFactory<IReportsTabLogic>> buttonFactories;
	
	private ReportIntervalType intervalType;

	private ReportIntervalBoundsMode intervalBoundsMode;
	
	private boolean initDone = false;
	
	private Calendar start = Calendar.getInstance();
	
	private Calendar end = Calendar.getInstance();
	
	public ReportsTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		// Add buttons
		registerButtons(buttonFactories);
		for (ReportIntervalType type : ReportIntervalType.values()) {
			getView().addIntervalTypeRadioButton(type, StringHelper.toLowerFirst(type.name().toLowerCase()));
		}
		for (ReportIntervalBoundsMode mode : ReportIntervalBoundsMode.values()) {
			getView().addIntervalBoundsModeRadioButton(mode, StringHelper.toLowerFirst(mode.name().replace('_',  ' ').toLowerCase()));
		}
		getView().selectIntervalTypeRadioButton(ReportIntervalType.MONTH);
		getView().selectIntervalBoundsModeButton(ReportIntervalBoundsMode.AUTOMATIC);
		initDone = true;
		updateFieldsEnablement();
	}

	@Override
	public String getLabel() {
		return "Reports";
	}

	@Override
	public void onIntervalTypeChanged(Object newValue) {
		intervalType = (ReportIntervalType) newValue;
		updateFieldsEnablement();
	}
	
	@Override
	public void onIntervalBoundsModeChanged(Object newValue) {
		intervalBoundsMode = (ReportIntervalBoundsMode) newValue;
		updateFieldsEnablement();
	}

	@Override
	public void onIntervalBoundsChanged(Date startDate, Date endDate) {
		start.setTime(startDate);
		end.setTime(endDate);
		updateFieldsEnablement();
	}

	void updateFieldsEnablement() {
		if (!initDone) {
			return;
		}
		// Update date fields enablement
		switch (intervalBoundsMode) {
		case AUTOMATIC:
			getView().setIntervalBoundsModeEnablement(false, false);
			break;
		case LOWER_BOUND:
			getView().setIntervalBoundsModeEnablement(true, false);
			break;
		case BOTH_BOUNDS:
			getView().setIntervalBoundsModeEnablement(true, true);
		}

		// Update dates
		if (!ReportIntervalType.DAY.equals(intervalType)) {
			switch (intervalType) {
			case YEAR:
				// Goto start of year
				start.set(Calendar.MONTH, 0);
				start.set(Calendar.DATE, 1);
				
				// Goto start of following year
				end.set(Calendar.MONTH, 0);
				end.set(Calendar.DATE, 1);
				end.add(Calendar.YEAR, 1);
				break;
			case MONTH:
				// Goto start of month
				start.set(Calendar.DATE, 1);
				
				// Goto start of following month
				end.set(Calendar.DATE, 1);
				end.add(Calendar.MONTH, 1);
				break;
			case WEEK:
				// Goto start of week
				start = DateHelper.moveToFirstDayOfWeek(start);
				
				// Goto start of following month
				end = DateHelper.moveToFirstDayOfWeek(end);
				end.add(Calendar.WEEK_OF_YEAR, 1);
				break;
			case DAY:
				// Do nothing
			}
			end.add(Calendar.DATE, -1);
			getView().setIntervalBounds(start.getTime(), end.getTime());
		}
	}
}
