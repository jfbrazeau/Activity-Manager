package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;

import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;

public abstract class AbstractContributionLogicImpl extends AbstractLogicImpl<IContributionsLogic.View> {

	public AbstractContributionLogicImpl(RootLogicImpl parent) {
		super(parent);
	}

	public abstract void addTask(long taskId);

	public abstract Calendar getFirstDayOfWeek();

}
