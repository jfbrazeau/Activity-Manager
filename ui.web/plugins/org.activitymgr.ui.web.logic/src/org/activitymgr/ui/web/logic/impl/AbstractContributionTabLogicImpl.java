package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;
import java.util.Collection;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;

public abstract class AbstractContributionTabLogicImpl extends AbstractTabLogicImpl<IContributionsTabLogic.View> implements IContributionsTabLogic {

	public AbstractContributionTabLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);
	}

	public abstract void addTask(long taskId);

	public abstract Calendar getFirstDayOfWeek();
	
	public abstract Collaborator getContributor();
	
	public abstract Collection<Long> getTaskIds();

}
