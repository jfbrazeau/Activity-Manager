package org.activitymgr.ui.web.logic;


public interface IRootLogic extends ILogic<IRootLogic.View> {
	
	IUILogicContext getContext();

	public interface View extends ILogic.IView<IRootLogic> {

		void showErrorNotification(String message, String details);
	
		void showNotification(String message);
		
		void showConfirm(String message, IGenericCallback<Boolean> callback);

		void simpleInput(String message, String defaultValue,
				IGenericCallback<String> callback);

		void setContentView(IView<?> contentView);

		String getCookie(String name);
		
		void setCookie(String name, String value);

		void removeCookie(String name);

		void openWindow(IView<?> view);

		void openExternalUrl(String url);

	}

}
