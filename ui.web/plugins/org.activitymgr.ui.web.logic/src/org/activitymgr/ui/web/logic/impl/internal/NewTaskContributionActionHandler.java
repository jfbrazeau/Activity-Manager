package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeContributionsActionHandler;

public class NewTaskContributionActionHandler extends
	AbstractSafeContributionsActionHandler {

	@Override
	public void unsafeHandle(AbstractContributionTabLogicImpl logic) {
		new TaskChooserLogicImpl(logic, logic.getTaskIds(), logic.getContributor(), logic.getFirstDayOfWeek());
	}

}
