package org.activitymgr.core.dao;

import java.util.Calendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Task;

public interface ICollaboratorDAO extends IDAO<Collaborator> {

	/**
	 * @param task
	 *            la tache associée aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return the contributors list corresponding to the given date interval.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator[] getContributors(Task task, Calendar fromDate, Calendar toDate)
			throws DAOException;

}
