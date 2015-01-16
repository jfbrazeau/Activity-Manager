package org.activitymgr.ui.web.logic;


public interface ICollaboratorsTabLogic extends ITabLogic<ICollaboratorsTabLogic.View> {

	public interface View extends ITabLogic.View<ICollaboratorsTabLogic> {

		void setCollaboratorsProviderCallback(
				ITableCellProviderCallback<Long> collaboratorsProviderCallback);
		
	}

}
