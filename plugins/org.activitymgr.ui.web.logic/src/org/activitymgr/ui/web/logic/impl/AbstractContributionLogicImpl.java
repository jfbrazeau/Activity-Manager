package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;
import java.util.List;

import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;

public abstract class AbstractContributionLogicImpl extends AbstractLogicImpl<IContributionsLogic.View> {

	public AbstractContributionLogicImpl(RootLogicImpl parent) {
		super(parent);
	}

	public abstract void addTask(Task task);
	
	public abstract void addTask(long taskId);

	public abstract Calendar getFirstDayOfWeek();
	
	public abstract List<TaskContributions> getWeekContributions();

	public abstract void addTask(Task task, String taskCodePath);

}
