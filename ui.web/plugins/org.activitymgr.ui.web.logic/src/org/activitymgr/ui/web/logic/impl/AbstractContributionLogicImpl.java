package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;
import java.util.List;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;

public abstract class AbstractContributionLogicImpl extends AbstractLogicImpl<IContributionsTabLogic.View> {

	public AbstractContributionLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);
	}

	public abstract void addTask(Task task);
	
	public abstract void addTask(long taskId);

	public abstract Calendar getFirstDayOfWeek();
	
	public abstract Collaborator getContributor();
	
	public abstract AbstractWeekContributionsProviderExtension getContributionsProvider();
	
	public abstract List<TaskContributions> getWeekContributions();

	public abstract void addTask(Task task, String taskCodePath);

}
