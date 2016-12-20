package org.activitymgr.ui.web.logic;


public interface ITasksTabLogic extends ITabLogic<ITasksTabLogic.View> {
	
	String ID = "tasks";

	void onTaskSelected(Object value);
	
	public interface View extends ITabLogic.View<ITasksTabLogic> {

		void setTreeContentProviderCallback(
				ITreeContentProviderCallback<Long> tasksProviderCallback);
		
	}

}
