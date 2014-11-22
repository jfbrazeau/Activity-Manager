package org.activitymgr.core.impl.dao;

import org.activitymgr.core.IBeanFactory;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.IDurationDAO;

import com.google.inject.Inject;

public class DurationDAOImpl extends AbstractORMDAOImpl<Duration> implements
		IDurationDAO {
	
	/** Bean factory */
	@Inject
	private IBeanFactory factory;
	
	@Override
	public void createDuration(long durationId) throws DAOException {
		Duration duration = factory.newDuration();
		duration.setId(durationId);
		insert(duration);
	}
}