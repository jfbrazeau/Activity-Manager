package org.activitymgr.core.impl.dao;

import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.IDurationDAO;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;

import com.google.inject.Inject;

public class DurationDAOImpl extends AbstractORMDAOImpl<Duration> implements
		IDurationDAO {
	
	/** Bean factory */
	@Inject
	private IDTOFactory factory;
	
	@Override
	public void createDuration(long durationId) throws DAOException {
		Duration duration = factory.newDuration();
		duration.setId(durationId);
		insert(duration);
	}
}