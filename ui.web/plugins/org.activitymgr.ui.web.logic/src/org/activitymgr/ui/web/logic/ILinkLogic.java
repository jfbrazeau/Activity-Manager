package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ILinkLogic extends ILogic<ILinkLogic.View> {
	
	public interface View extends IView<ILinkLogic> {
		
		void setLabel(String s);
		
		void setHref(String href);

	}

}
