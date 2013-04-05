package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IAuthenticationLogic extends ILogic<IAuthenticationLogic.View> {
	
	public void onAuthenticate(String login, String password);
	
	public interface View extends IView<IAuthenticationLogic> {
		
	}

}
