package org.activitymgr.ui.web.logic.spi;

public interface IAuthenticatorExtension {

	boolean authenticate(String login, String password);

}
