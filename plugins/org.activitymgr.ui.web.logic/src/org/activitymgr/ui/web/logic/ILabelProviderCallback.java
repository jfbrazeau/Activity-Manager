package org.activitymgr.ui.web.logic;

public interface ILabelProviderCallback {

	enum Icon {
		ERROR, EOBJECT, REPOSITORY_ON, REPOSITORY_OFF, BRANCH, FOLDER, FILE
	}

	String getText();

	Icon getIcon();

}
