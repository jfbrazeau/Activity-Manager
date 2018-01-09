package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Calendar;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeContributionTabStandardButtonLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.ContributionsTabWeekChangedEvent;
import org.activitymgr.ui.web.logic.spi.IFeatureAccessManager;

import com.google.inject.Inject;

public class NewContributionTaskButtonLogic extends AbstractSafeContributionTabStandardButtonLogicImpl {
	
	@Inject
	private IFeatureAccessManager featureAccessManager;

	@Inject
	public NewContributionTaskButtonLogic(IContributionsTabLogic parent) {
		super(parent,"New task", "new", "CTRL+SHIFT+N");
	}

	@Override
	protected void unsafeOnClick() {
		AbstractContributionTabLogicImpl contributionTabLogic = (AbstractContributionTabLogicImpl) getParent();
		Collaborator contributor = contributionTabLogic.getContributor();

		// Retrieve recent tasks labels
		Calendar monday = contributionTabLogic.getFirstDayOfWeek();
		Calendar from = (Calendar) monday.clone();
		from.add(Calendar.DATE, -7);
		Calendar to = (Calendar) monday.clone();
		to.add(Calendar.DATE, 6);
		Task[] recentTasks = getModelMgr().getContributedTasks(contributor,
				from, to);

		new ContributionTaskChooserLogicImpl(contributionTabLogic,
				contributionTabLogic.getLastSelectedTaskId(),
				contributionTabLogic.getTaskIds(), recentTasks);
	}

	@Override
	protected boolean mustBeEnabled(ContributionsTabWeekChangedEvent event) {
		return featureAccessManager.canUpdateContributions(getContext().getConnectedCollaborator(), event.getContributor());
	}
}
