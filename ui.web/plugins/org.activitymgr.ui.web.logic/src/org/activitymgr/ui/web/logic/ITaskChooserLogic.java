package org.activitymgr.ui.web.logic;

import java.util.Map;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITaskChooserLogic extends ILogic<ITaskChooserLogic.View> {
	
	void onSelectionChanged(long taskId);

	void onOkButtonClicked(long taskId);

	void onRecentTaskClicked(long taskId);
	
	void onNewTaskCheckboxClicked();

	void onNewTaskNameChanged(String newName);

	void onNewTaskCodeChanged(String newCode);

	public interface View extends IView<ITaskChooserLogic> {

		public void setTasksTreeProviderCallback(
				ITreeContentProviderCallback<Long> callback);

		public void setRecentTasks(Map<Long, String> recentTasks);

		public void setCreationPatterns(Map<String, String> patterns);

		public void setOkButtonEnabled(boolean enabled);

		public void setStatus(String status);

		public void selectTask(long taskId);

		public boolean isNewTaskChecked();

		public String getNewTaskName();
		
		public String getNewTaskCode();
		
		public long getSelectedTaskId();

		public void setNewTaskFieldsEnabled(boolean enabled);

		public String getSelectedTaskCreationPatternId();
	}

}
