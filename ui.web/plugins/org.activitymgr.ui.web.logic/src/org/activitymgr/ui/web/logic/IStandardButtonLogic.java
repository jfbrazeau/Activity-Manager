package org.activitymgr.ui.web.logic;

public interface IStandardButtonLogic extends IButtonLogic<IStandardButtonLogic.View> {

	void onClick();
	
	public interface View extends IButtonLogic.View<IStandardButtonLogic> {
		
		void setShortcut(char key,
				boolean ctrl, boolean shift, boolean alt);

	}

}
