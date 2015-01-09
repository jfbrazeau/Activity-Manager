package org.activitymgr.ui.web.logic.impl;

public abstract class AbstractSafeContributionsActionHandler implements IContributionsActionHandler {
	
	public final void handle(AbstractContributionTabLogicImpl logic) {
		try {
			unsafeHandle(logic);
		}
		catch (Throwable t) {
			logic.handleError(t);
		}
	}

	public abstract void unsafeHandle(AbstractContributionTabLogicImpl logic) throws Exception;
}
