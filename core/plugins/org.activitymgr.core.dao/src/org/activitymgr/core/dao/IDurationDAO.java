package org.activitymgr.core.dao;

import org.activitymgr.core.dto.Duration;

public interface IDurationDAO extends IDAO<Duration> {

	void createDuration(long durationId) throws DAOException;
	
}
