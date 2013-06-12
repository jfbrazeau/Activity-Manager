package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IRootLogic extends ILogic<IRootLogic.View> {
	
	public interface View extends IView<IRootLogic> {

		void showErrorNotification(String message, String details);
	
		void showNotification(String message);
		
		void showConfirm(String message, IGenericCallback<Boolean> callback);

		void show(IView<?> view);

		String getCookie(String name);
		
		void setCookie(String name, String value);

		void removeCookie(String name);
	}

}
