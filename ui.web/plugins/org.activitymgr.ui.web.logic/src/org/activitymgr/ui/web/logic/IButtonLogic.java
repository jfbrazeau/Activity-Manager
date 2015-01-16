package org.activitymgr.ui.web.logic;

public interface IButtonLogic extends ILogic<IButtonLogic.View> {
	
	void onClick();
	
	public interface View extends ILogic.IView<IButtonLogic> {
		
		void setIcon(String iconId);
		
		void setDescription(String caption);

	}

}
