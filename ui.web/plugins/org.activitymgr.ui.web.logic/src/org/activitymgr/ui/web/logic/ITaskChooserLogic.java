package org.activitymgr.ui.web.logic;

public interface ITaskChooserLogic<VIEW extends ITaskChooserLogic.View<?>>
		extends ILogic<VIEW> {
	
	void onSelectionChanged(Long taskId);

	void onOkButtonClicked(long taskId);

	void onTaskFilterChanged(String value);

	public interface View<LOGIC extends ITaskChooserLogic<?>> extends
			ILogic.IView<LOGIC> {

		public void setTasksTreeProviderCallback(
				ITreeContentProviderCallback<Long> callback);

		public void setOkButtonEnabled(boolean enabled);

		public void setStatus(String status);

		public void selectTask(long taskId);

		public void expandToTask(long taskId);

	}

}
