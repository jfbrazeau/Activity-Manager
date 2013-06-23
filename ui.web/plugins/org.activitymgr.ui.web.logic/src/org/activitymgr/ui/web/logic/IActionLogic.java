package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IActionLogic extends ILogic<IActionLogic.View> {
	
	void onActionInvoked();
	
	public interface View extends IView<IActionLogic> {
		
		void setLabel(String label);

	}

}
