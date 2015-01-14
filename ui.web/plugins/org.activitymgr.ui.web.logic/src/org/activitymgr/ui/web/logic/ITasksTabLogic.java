package org.activitymgr.ui.web.logic;


public interface ITasksTabLogic extends IActionLogic<ITasksTabLogic.View> {

	public interface View extends IActionLogic.View<ITasksTabLogic> {

		void setTreeContentProviderCallback(
				ITreeContentProviderCallback<Long> tasksProviderCallback);
		
	}

}
