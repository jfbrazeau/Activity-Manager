package org.activitymgr.ui.web.logic;

import java.util.Date;


public interface IReportsTabLogic extends ITabLogic<IReportsTabLogic.View> {
	
	String ID = "reports";
	
	void onIntervalTypeChanged(Object newValue);

	void onIntervalBoundsModeChanged(Object newValue);

	void onIntervalBoundsChanged(Date startDate, Date endDate);

	void onBrowseTaskButtonCLicked();

	void onTaskScopePathChanged(String value);

	void onTaskTreeDepthChanged(int parseInt);

	void onOnlyKeepTaskWithContributionsCheckboxChanged(boolean value);

	public interface View extends ITabLogic.View<IReportsTabLogic> {
		
		void addIntervalTypeRadioButton(Object id, String label);
		
		void selectIntervalTypeRadioButton(Object id);

		void addIntervalBoundsModeRadioButton(Object id, String label);

		void selectIntervalBoundsModeButton(Object id);

		void setIntervalBoundsModeEnablement(boolean startDateEnablement, boolean endDateEnablement);

		void setIntervalBounds(Date startDate, Date endDate);

		void setColumnSelectionView(ITwinSelectFieldLogic.View view);

		void setCollaboratorsSelectionView(ITwinSelectFieldLogic.View view);

		void setTaskScopePath(String path);

		void setTaskTreeDepth(int i);

		void setBuildReportButtonView(IDownloadButtonLogic.View view);

		void setBuildReportButtonEnabled(boolean b);

		void setErrorMessage(String message);

		void setRowContentConfigurationEnabled(boolean includeTaskAttrs);

	}

}
