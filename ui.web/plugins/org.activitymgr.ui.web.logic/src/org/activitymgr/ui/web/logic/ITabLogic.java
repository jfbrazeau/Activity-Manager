package org.activitymgr.ui.web.logic;


public interface ITabLogic<VIEW extends ITabLogic.View<?>> extends ILogic<VIEW> {
	
	String getLabel();

	public interface View<LOGIC extends ITabLogic<?>> extends IView<LOGIC> {

		void addButton(IButtonLogic.View<?> buttonView);
		
	}

}
