package org.activitymgr.ui.web.logic;

public interface IFieldLogic<T, VIEW extends IFieldLogic.View<T, ?>> extends ILogic<VIEW> {
	
	void onValueChanged(T newValue);
	
	public interface View<T, LOGIC extends IFieldLogic<T, ?>> extends IFieldLogic.IView<LOGIC> {
		
		void focus();
		
		void setValue(T value);
		
		void setReadOnly(boolean readOnly);
		
		boolean isReadOnly();

	}

}
