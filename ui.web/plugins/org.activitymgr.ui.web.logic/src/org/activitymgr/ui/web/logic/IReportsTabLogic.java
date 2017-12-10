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

	public interface View extends ITabLogic.View<IReportsTabLogic> {
		
		void addIntervalTypeRadioButton(Object id, String label);
		
		void selectIntervalTypeRadioButton(Object id);

		void addIntervalBoundsModeRadioButton(Object id, String label);

		void selectIntervalBoundsModeButton(Object id);

		void setIntervalBoundsModeEnablement(boolean startDateEnablement, boolean endDateEnablement);

		void setIntervalBounds(Date startDate, Date endDate);

		void setColumnSelectionView(ITwinSelectLogic.View view);

		void setCollaboratorsSelectionView(ITwinSelectLogic.View view);

		void setTaskScopePath(String path);

		void setTaskTreeDepth(int i);

		void setBuildReportButtonView(IDownloadButtonLogic.View view);

		void setBuildReportButtonEnabled(boolean b);

		void setErrorMessage(String message);

	}

}
