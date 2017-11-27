package org.activitymgr.ui.web.logic;


public interface ILinkLogic extends ILogic<ILinkLogic.View> {
	
	public interface View extends ILogic.IView<ILinkLogic> {
		
		void setLabel(String s);
		
		void setHref(String href);

	}

}
