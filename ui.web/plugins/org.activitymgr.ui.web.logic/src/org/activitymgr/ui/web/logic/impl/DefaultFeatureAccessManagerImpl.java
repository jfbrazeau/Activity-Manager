package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.ui.web.logic.IFeatureAccessManager;

public class DefaultFeatureAccessManagerImpl implements IFeatureAccessManager {

	@Override
	public boolean hasAccessToTab(Collaborator collaborator, String tab) {
		return true;
	}

}
