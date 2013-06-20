package org.activitymgr.ui.web.logic.impl;

public abstract class AbstractSafeContributionsActionHandler implements IContributionsActionHandler {
	
	public final void handle(AbstractContributionLogicImpl logic) {
		try {
			unsafeHandle(logic);
		}
		catch (Throwable t) {
			logic.handleError(t);
		}
	}

	public abstract String getLabel();
	
	public abstract void unsafeHandle(AbstractContributionLogicImpl logic) throws Exception;
}
