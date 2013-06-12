package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ILabelLogic extends ILogic<ILabelLogic.View> {
	
	public interface View extends IView<ILabelLogic> {
		
		void setLabel(String s);

	}

}
