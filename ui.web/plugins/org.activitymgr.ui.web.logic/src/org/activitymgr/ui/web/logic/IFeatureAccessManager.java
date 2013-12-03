package org.activitymgr.ui.web.logic;

import org.activitymgr.core.beans.Collaborator;

public interface IFeatureAccessManager {
	
	boolean hasAccessToTab(Collaborator collaborator, String tab);

}
