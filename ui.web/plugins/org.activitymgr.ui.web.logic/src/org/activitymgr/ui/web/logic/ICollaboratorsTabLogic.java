package org.activitymgr.ui.web.logic;


public interface ICollaboratorsTabLogic extends IActionLogic<ICollaboratorsTabLogic.View> {

	public interface View extends IActionLogic.View<ICollaboratorsTabLogic> {

		void setCollaboratorsProviderCallback(
				ITableCellProviderCallback<Long> collaboratorsProviderCallback);
		
	}

}
