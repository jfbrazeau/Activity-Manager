package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeContributionTabStandardButtonLogicImpl;

import com.google.inject.Inject;

public class NewContributionTaskButtonLogic extends AbstractSafeContributionTabStandardButtonLogicImpl {

	@Inject
	public NewContributionTaskButtonLogic(IContributionsTabLogic parent) {
		super(parent,"New task", "new", "CTRL+SHIFT+N");
	}

	@Override
	protected void unsafeOnClick() {
		AbstractContributionTabLogicImpl contributionTabLogic = (AbstractContributionTabLogicImpl) getParent();
		new TaskChooserLogicImpl(contributionTabLogic,
				contributionTabLogic.getTaskIds(),
				contributionTabLogic.getContributor(),
				contributionTabLogic.getFirstDayOfWeek());
	}

}
