package org.activitymgr.ui.web.logic;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITaskChooserLogic extends ILogic<ITaskChooserLogic.View> {
	
	void onSelectionChanged(Long taskId);

	void onTaskChosen(long taskId);

	void onRecentTaskClicked(long taskId);
	
	public interface View extends IView<ITaskChooserLogic> {

		public void setTreeContentProviderCallback(
				ITreeContentProviderCallback callback);

		public void setRecentTasksProviderCallback(IListContentProviderCallback callback);

		public void setOkButtonEnabled(boolean enabled);

		public void setNewTaskFormEnabled(boolean enabled);

		public void setStatus(String status);

		public void expandTasks(Collection<Long> taskIds);

		public void selectTask(long taskId);

		public boolean isNewTaskChecked();

		public String getNewTaskName();

		/**
		 * This pre load trigger is required with vaadin otherwise, when one clicks on a recently selected task, the task does not get selected (due to vaadin).
		 * @param ids the tree items identifiers.
		 */
		public void preloadTreeItems(Collection<String> ids);
	}

}
