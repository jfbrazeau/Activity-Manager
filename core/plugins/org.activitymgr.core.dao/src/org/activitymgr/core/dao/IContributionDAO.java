package org.activitymgr.core.dao;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskContributionsSums;

public interface IContributionDAO extends IDAO<Contribution> {

	/**
	 * @param contributor
	 *            le collaborateur associé aux contributions.
	 * @param task
	 *            la tache associée aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la liste des contributions associées aux paramétres spécifiés.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException;

	/**
	 * @param contributor
	 *            le collaborateur associé aux contributions.
	 * @param task
	 *            la tache associée aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return le nombre de contributions.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException;

	/**
	 * Calcule le total des contributions associée aux paramétres spécifiés.
	 * 
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la seomme des contributions.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException;

	/**
	 * The sub tasks sums (consumed, ...) for a given task (when
	 * <code>taskId</code> is specified) or a set of tasks specified by their
	 * path.
	 * 
	 * @param taskId
	 *            the task identifier for which we want to know the sums.
	 * @param tasksPath
	 *            the tasks path for which we want to know the sums.
	 * @param fromDate
	 *            start of the date interval to consider
	 * @param toDate
	 *            end of the date interval to consider
	 * @return the sub tasks sums (consumed, ...)
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Map<Long, TaskContributionsSums> getTasksSums(Long taskId, String tasksPath, Calendar fromDate, Calendar toDate)
			throws DAOException;
	
	/**
	 * Return the contribution years list.
	 * 
	 * @return the contribution years list.
	 */
	Collection<Integer> getContributionYears();

	/**
	 * Returns the contributions interval.
	 * @param taskPath the optional task path to filter the contributions.
	 * @return the contributions interval.
	 */
	Calendar[] getContributionsInterval(String taskPath);
}
