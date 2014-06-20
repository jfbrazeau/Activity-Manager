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

		public void setStatus(String status);

		public void expandTask(long taskId);

		public void expandTasks(Collection<Long> taskIds);

		public void selectTask(long taskId);

	}

}
