package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITextFieldLogic extends ILogic<ITextFieldLogic.View> {
	
	void onValueChanged(String newValue);
	
	void onClick();

	void onEnterKeyPressed();
	
	public interface View extends IView<ITextFieldLogic> {
		
		void setValue(String value);

		void selectAll();

		void focus();
		
		void setNumericFieldStyle();

		void setReadOnly(boolean readOnly);
		
		boolean isReadOnly();

		void blur();
		
	}

}
