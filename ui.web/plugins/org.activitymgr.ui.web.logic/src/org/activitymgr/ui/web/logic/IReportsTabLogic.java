package org.activitymgr.ui.web.logic;

import java.util.Collection;


public interface IReportsTabLogic extends ITabLogic<IReportsTabLogic.View> {
	
	String ADVANCED_REPORTS_ID = "reports";
	
	String MY_REPORTS_ID = "myReports";

	void onSelectionChanged(Collection<Long> value);

	public interface View extends ITabLogic.View<IReportsTabLogic> {
		
		void setReportsView(IReportsLogic.View view);
		
		void setLongReportsList(boolean longList);

		void addReportConfigurationButton(
				org.activitymgr.ui.web.logic.IStandardButtonLogic.View view);

		void addReportCfg(long id, String name, int position);

		void setReportsPanelEnabled(boolean b);

		void selectReportCfg(long id);

		void removeReportCfg(long id);

	}

}
