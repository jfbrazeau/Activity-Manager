package org.activitymgr.ui.web.logic;

import java.util.Date;

public interface IReportsLogic extends ILogic<IReportsLogic.View> {
	
	void onIntervalTypeChanged(Object newValue);

	void onIntervalBoundsModeChanged(Object newValue);

	void onIntervalBoundsChanged(Date startDate, Date endDate);

	void onCollaboratorsSelectionModeChanged(Object value);

	void onBrowseTaskButtonCLicked();

	void onTaskScopePathChanged(String value);

	void onTaskTreeDepthChanged(int parseInt);

	void onOnlyKeepTaskWithContributionsCheckboxChanged(boolean value);

	void onIntervalCountChanged(int intervalCount);

	public interface View extends ILogic.IView<IReportsLogic> {
		
		void initialize(boolean advancedMode);

		void addIntervalTypeRadioButton(Object id, String label);

		void selectIntervalTypeRadioButton(Object id);

		void addIntervalBoundsModeRadioButton(Object id, String label);

		void selectIntervalBoundsModeButton(Object id);

		void setIntervalBoundsModeEnablement(boolean startDateEnablement,
				boolean endDateEnablement);

		void addCollaboratorsSelectionModeRadioButton(Object id, String label);

		void selectCollaboratorsSelectionModeRadioButton(Object newValue);

		void setIntervalBounds(Date startDate, Date endDate);

		void setIntervalCount(int intervalCount);

		void setCollaboratorsSelectionView(ITwinSelectFieldLogic.View view);

		void setCollaboratorsSelectionUIEnabled(boolean enabled);

		void setColumnSelectionView(ITwinSelectFieldLogic.View view);

		void setTaskScopePath(String path);

		void setTaskTreeDepth(int i);

		void setOnlyKeepTaskWithContributions(
				boolean onlyKeepTaskWithContributions);

		void addReportButton(IButtonLogic.View<?> view);

		void setErrorMessage(String message);

		void setRowContentConfigurationEnabled(boolean includeTaskAttrs);


	}

}
