package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeButtonLogicImpl;

public class NewContributionTaskButtonLogic extends AbstractSafeButtonLogicImpl {

	public NewContributionTaskButtonLogic(AbstractLogicImpl<?> parent) {
		super(parent);
	}

	@Override
	public void unsafeOnClick() {
		AbstractContributionTabLogicImpl contributionTabLogic = (AbstractContributionTabLogicImpl) getParent();
		new TaskChooserLogicImpl(contributionTabLogic,
				contributionTabLogic.getTaskIds(),
				contributionTabLogic.getContributor(),
				contributionTabLogic.getFirstDayOfWeek());
	}

}
