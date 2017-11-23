package org.activitymgr.ui.web.logic;


public interface IReportsTabLogic extends ITabLogic<IReportsTabLogic.View> {
	
	String ID = "reports";
	
	void onIntervalTypeChanged(Object newValue);

	void onIntervalBoundsModeChanged(Object newValue);

	public interface View extends ITabLogic.View<IReportsTabLogic> {
		
		void addIntervalTypeRadioButton(Object id, String label);
		
		void selectIntervalTypeRadioButton(Object id);

		void addIntervalBoundsModeRadioButton(Object id, String label);

		void selectIntervalBoundsModeButton(Object id);

	}

}
