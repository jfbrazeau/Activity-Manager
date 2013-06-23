package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITaskChooserLogic extends ILogic<ITaskChooserLogic.View> {
	
	void onSelectionChanged(Long taskId);

	void onTaskChosen(long taskId);

	public interface View extends IView<ITaskChooserLogic> {

		public void setTreeContentProviderCallback(
				ITreeContentProviderCallback treeContentProviderCallback);

		public void setOkButtonEnabled(boolean enabled);

		public void setStatus(String status);

	}

}
