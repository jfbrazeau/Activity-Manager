package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.IExternalContentDialogLogic.View;

public interface IExternalContentDialogLogic extends ILogic<View> {
	
	public interface View extends ILogic.IView<IExternalContentDialogLogic> {

		void setTitle(String title);
		
		void setContentUrl(String url);

	}

}
