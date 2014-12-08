package org.activitymgr.ui.web.logic;


public interface ITasksTabLogic extends ILogic<ITasksTabLogic.View> {

	public interface View extends ILogic.IView<ITasksTabLogic> {

		void setTreeContentProviderCallback(
				ITreeContentProviderCallback<Long> tasksProviderCallback);
		
	}

}
