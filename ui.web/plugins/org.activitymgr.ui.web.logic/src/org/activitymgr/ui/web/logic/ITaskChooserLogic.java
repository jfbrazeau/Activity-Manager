package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITaskChooserLogic extends ILogic<ITaskChooserLogic.View> {
	
	void onSelectionChanged(long taskId);

	void onOkButtonClicked(long taskId);

	void onRecentTaskClicked(long taskId);
	
	void onNewTaskCheckboxClicked();

	void onNewTaskNameChanged(String newName);

	public interface View extends IView<ITaskChooserLogic> {

		public void setTreeContentProviderCallback(
				ITreeContentProviderCallback<Long> callback);

		public void setRecentTasksProviderCallback(ITableCellProviderCallback<Long> callback);

		public void setOkButtonEnabled(boolean enabled);

		public void setNewTaskFormEnabled(boolean enabled);

		public void setStatus(String status);

		public void selectTask(long taskId);

		public boolean isNewTaskChecked();

		public String getNewTaskName();
		
		public long getSelectedTaskId();

		public void setNewTaskNameEnabled(boolean enabled);
	}

}
