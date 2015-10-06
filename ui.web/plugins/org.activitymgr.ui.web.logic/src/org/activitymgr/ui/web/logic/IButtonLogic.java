package org.activitymgr.ui.web.logic;

public interface IButtonLogic<VIEW extends IButtonLogic.View<?>> extends ILogic<VIEW> {
	
	public interface View<LOGIC extends IButtonLogic<?>> extends IButtonLogic.IView<LOGIC> {
		
		void setIcon(String iconId);
		
		void setDescription(String caption);
		
	}

}
