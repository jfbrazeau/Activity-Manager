package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITextFieldLogic extends ILogic<ITextFieldLogic.View> {
	
	void onValueChanged(String newValue);
	
	public interface View extends IView<ITextFieldLogic> {
		
		void setValue(String value);

		void focus();

		void setNumericFieldStyle();

		void setReadOnly(boolean readOnly);
		
	}

}
