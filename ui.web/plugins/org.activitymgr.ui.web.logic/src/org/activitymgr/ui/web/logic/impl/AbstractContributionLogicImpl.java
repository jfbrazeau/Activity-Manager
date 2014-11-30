package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;

public abstract class AbstractContributionLogicImpl extends AbstractLogicImpl<IContributionsTabLogic.View> {

	public AbstractContributionLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);
	}

	public abstract void addTask(Task task);
	
	public abstract void addTask(long taskId);

	public abstract Calendar getFirstDayOfWeek();
	
	public abstract Collaborator getContributor();
	
	public abstract List<TaskContributions> getWeekContributions();

	public abstract void addTask(Task task, String taskCodePath);

}
