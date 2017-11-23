package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Set;

import org.activitymgr.core.dto.report.ReportIntervalType;
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
	
	public ReportsTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		// Add buttons
		registerButtons(buttonFactories);
		for (ReportIntervalType type : ReportIntervalType.values()) {
			getView().addIntervalTypeRadioButton(type, StringHelper.toLowerFirst(type.name().toLowerCase()));
		}
		getView().selectIntervalTypeRadioButton(ReportIntervalType.MONTH);
		for (ReportIntervalBoundsMode mode : ReportIntervalBoundsMode.values()) {
			getView().addIntervalBoundsModeRadioButton(mode, StringHelper.toLowerFirst(mode.name().replace('_',  ' ').toLowerCase()));
		}
		getView().selectIntervalBoundsModeButton(ReportIntervalBoundsMode.AUTOMATIC);
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

	void updateFieldsEnablement() {
		System.out.println(intervalType + " / " + intervalBoundsMode);
		
	}
}
