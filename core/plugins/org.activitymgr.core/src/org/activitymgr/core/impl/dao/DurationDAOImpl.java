package org.activitymgr.core.impl.dao;

import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.IDurationDAO;

public class DurationDAOImpl extends AbstractORMDAOImpl<Duration> implements
		IDurationDAO {
	
	@Override
	public void createDuration(long durationId) throws DAOException {
		Duration duration = new Duration();
		duration.setId(durationId);
		insert(duration);
	}
}