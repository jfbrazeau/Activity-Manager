package org.activitymgr.ui.web.logic;

import java.util.Map;

public interface IContributionTaskChooserLogic extends
		ITaskChooserLogic<IContributionTaskChooserLogic.View> {
	
	void onRecentTaskClicked(long taskId);
	
	void onNewTaskCheckboxClicked();

	void onNewTaskNameChanged(String newName);

	void onNewTaskCodeChanged(String newCode);

	public interface View extends
			ITaskChooserLogic.View<IContributionTaskChooserLogic> {

		public void setTasksTreeProviderCallback(
				ITreeContentProviderCallback<Long> callback);

		public void setRecentTasks(Map<Long, String> recentTasks);

		public void setCreationPatterns(Map<String, String> patterns);

		public void setNewTaskFieldsEnabled(boolean enabled);

		public String getSelectedTaskCreationPatternId();

	}

}
