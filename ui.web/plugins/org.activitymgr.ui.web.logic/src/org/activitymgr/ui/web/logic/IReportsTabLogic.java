package org.activitymgr.ui.web.logic;


public interface IReportsTabLogic extends ITabLogic<IReportsTabLogic.View> {
	
	String ADVANCED_REPORTS_ID = "reports";
	
	String MY_REPORTS_ID = "myReports";

	public interface View extends ITabLogic.View<IReportsTabLogic> {
		
		void setReportsView(IReportsLogic.View view);

	}

}
