package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;
import java.util.Collection;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ILogic;

public abstract class AbstractContributionTabLogicImpl extends AbstractLogicImpl<IContributionsTabLogic.View> implements IContributionsTabLogic {

	public AbstractContributionTabLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);
	}

	public abstract void addTask(long taskId);

	public abstract Calendar getFirstDayOfWeek();
	
	public abstract Collaborator getContributor();
	
	public abstract Collection<Long> getTaskIds();

	public abstract TaskContributions getWeekContributions(long taskId);

	public abstract void setFooter(String propertyId, String text);

	public abstract ILogic<?> getCellLogic(long taskId, String propertyId);

}
