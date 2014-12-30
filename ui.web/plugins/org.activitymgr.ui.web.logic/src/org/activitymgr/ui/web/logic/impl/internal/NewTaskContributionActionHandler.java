package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.impl.AbstractContributionLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeContributionsActionHandler;

public class NewTaskContributionActionHandler extends
	AbstractSafeContributionsActionHandler {

	@Override
	public void unsafeHandle(AbstractContributionLogicImpl logic) {
		new TaskChooserLogicImpl(logic, logic.getTaskIds(), logic.getContributor(), logic.getFirstDayOfWeek());
	}

}
