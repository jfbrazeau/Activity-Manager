package org.activitymgr.ui.web.logic;


public interface ITextFieldLogic extends IFieldLogic<String, ITextFieldLogic.View> {
	
	void onClick();

	void onEnterKeyPressed();
	
	public interface View extends IFieldLogic.View<String, ITextFieldLogic> {
		
		void blur();
		
		void setTooltip(String text);

		void selectAll();

		void setNumericFieldStyle();

		void setReadOnly(boolean readOnly);
		
		boolean isReadOnly();

	}

}
