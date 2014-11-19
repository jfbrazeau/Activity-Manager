package org.activitymgr.core.dao;

import org.activitymgr.core.beans.Duration;

public interface IDurationDAO extends IDAO<Duration> {

	void createDuration(long durationId) throws DAOException;
	
}
