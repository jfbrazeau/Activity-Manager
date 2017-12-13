package org.activitymgr.ui.web.logic.spi;

import org.activitymgr.core.dto.Collaborator;

public interface IFeatureAccessManager {
	
	boolean hasAccessToTab(Collaborator connected, String tab);
	
	/**
	 * Tells whether a button is enabled for a given connected user when
	 * accessing to a given collaborator's week contributions.
	 * 
	 * @param connected
	 *            the connected user.
	 * @param contributor
	 *            the contributor.
	 * @return <code>true</code> if the connected collaborator can update the
	 *         contributor's contributions.
	 */
	boolean canUpdateContributions(Collaborator connected, Collaborator contributor);

}
