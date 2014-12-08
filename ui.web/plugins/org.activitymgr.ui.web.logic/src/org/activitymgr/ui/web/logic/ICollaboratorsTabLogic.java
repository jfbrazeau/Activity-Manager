package org.activitymgr.ui.web.logic;


public interface ICollaboratorsTabLogic extends ILogic<ICollaboratorsTabLogic.View> {

	public interface View extends ILogic.IView<ICollaboratorsTabLogic> {

		void setCollaboratorsProviderCallback(
				IListContentProviderCallback<Long> collaboratorsProviderCallback);
		
	}

}
