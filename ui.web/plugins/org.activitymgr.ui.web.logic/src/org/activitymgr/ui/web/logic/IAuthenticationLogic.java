package org.activitymgr.ui.web.logic;

public interface IAuthenticationLogic extends ILogic<IAuthenticationLogic.View> {
	
	public void onAuthenticate(String login, String password, boolean rememberMe);
	
	public void onAuthenticateWithGoogle(String idToken);

	void onViewAttached();
	
	public interface View extends ILogic.IView<IAuthenticationLogic> {

		void setGoogleSignInClientId(String googleSignInClientId);
		
	}

}
