package org.activitymgr.ui.web.logic;

import java.util.Date;


public interface IReportsTabLogic extends ITabLogic<IReportsTabLogic.View> {
	
	String ADVANCED_REPORTS_ID = "reports";
	
	String MY_REPORTS_ID = "myReports";

	void onIntervalTypeChanged(Object newValue);

	void onIntervalBoundsModeChanged(Object newValue);

	void onIntervalBoundsChanged(Date startDate, Date endDate);

	void onCollaboratorsSelectionModeChanged(Object value);

	void onBrowseTaskButtonCLicked();

	void onTaskScopePathChanged(String value);

	void onTaskTreeDepthChanged(int parseInt);

	void onOnlyKeepTaskWithContributionsCheckboxChanged(boolean value);

	public interface View extends ITabLogic.View<IReportsTabLogic> {
		
		void initialize(boolean advancedMode);

		void addIntervalTypeRadioButton(Object id, String label);
		
		void selectIntervalTypeRadioButton(Object id);

		void addIntervalBoundsModeRadioButton(Object id, String label);

		void selectIntervalBoundsModeButton(Object id);

		void setIntervalBoundsModeEnablement(boolean startDateEnablement, boolean endDateEnablement);

		void addCollaboratorsSelectionModeRadioButton(Object id, String label);

		void selectCollaboratorsSelectionModeRadioButton(Object newValue);

		void setIntervalBounds(Date startDate, Date endDate);

		void setCollaboratorsSelectionView(ITwinSelectFieldLogic.View view);

		void setCollaboratorsSelectionUIEnabled(boolean enabled);

		void setColumnSelectionView(ITwinSelectFieldLogic.View view);

		void setTaskScopePath(String path);

		void setTaskTreeDepth(int i);

		void setBuildReportButtonView(IDownloadButtonLogic.View view);

		void setBuildReportButtonEnabled(boolean b);

		void setErrorMessage(String message);

		void setRowContentConfigurationEnabled(boolean includeTaskAttrs);

	}

}
