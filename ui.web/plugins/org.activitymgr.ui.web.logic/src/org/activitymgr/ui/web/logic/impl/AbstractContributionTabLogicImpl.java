package org.activitymgr.ui.web.logic.impl;

import java.util.Calendar;
import java.util.Collection;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;

public abstract class AbstractContributionTabLogicImpl extends AbstractTabLogicImpl<IContributionsTabLogic.View> implements IContributionsTabLogic {

	public AbstractContributionTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
	}

	public abstract void addTasks(long... taskIds);

	public abstract Calendar getFirstDayOfWeek();
	
	public abstract Collaborator getContributor();
	
	public abstract Collection<Long> getTaskIds();


}
