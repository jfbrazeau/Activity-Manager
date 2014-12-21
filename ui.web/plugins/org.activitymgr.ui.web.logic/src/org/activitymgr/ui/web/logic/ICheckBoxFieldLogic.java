package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ICheckBoxFieldLogic extends ILogic<ICheckBoxFieldLogic.View> {
	
	void onValueChanged(boolean newValue);
	
	public interface View extends IView<ICheckBoxFieldLogic> {
		
		void setValue(boolean value);

		void focus();
		
	}

}
