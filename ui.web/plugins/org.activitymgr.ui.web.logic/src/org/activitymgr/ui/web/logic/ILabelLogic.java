package org.activitymgr.ui.web.logic;


public interface ILabelLogic extends ILogic<ILabelLogic.View> {
	
	public interface View extends ILogic.IView<ILabelLogic> {
		
		void setLabel(String s);
		
		String getLabel();
		
	}

}
