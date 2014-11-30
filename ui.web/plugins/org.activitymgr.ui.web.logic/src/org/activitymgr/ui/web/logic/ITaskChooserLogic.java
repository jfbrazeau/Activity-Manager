package org.activitymgr.ui.web.logic;

import java.util.Collection;

import org.activitymgr.core.dto.Task;
import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITaskChooserLogic extends ILogic<ITaskChooserLogic.View> {
	
	void onSelectionChanged(Task task);

	void onOkButtonClicked(Task task);

	void onRecentTaskClicked(Task task);
	
	void onNewTaskCheckboxClicked();

	void onNewTaskNameChanged(String newName);

	public interface View extends IView<ITaskChooserLogic> {

		public void setTreeContentProviderCallback(
				ITreeContentProviderCallback<?> callback);

		public void setRecentTasksProviderCallback(IListContentProviderCallback<Task> callback);

		public void setOkButtonEnabled(boolean enabled);

		public void setNewTaskFormEnabled(boolean enabled);

		public void setStatus(String status);

		public void expandTasks(Collection<Task> tasks);

		public void selectTask(Task task);

		public boolean isNewTaskChecked();

		public String getNewTaskName();
		
		public Task getSelectedTask();

		/**
		 * This pre load trigger is required with vaadin otherwise, when one clicks on a recently selected task, the task does not get selected (due to vaadin).
		 * @param ids the tree items identifiers.
		 */
		public void preloadTreeItems(Collection<Task> tasks);

		public void setNewTaskNameEnabled(boolean enabled);
	}

}
