package org.activitymgr.core.dao;

import java.util.Calendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSums;

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
	 * @param task
	 *            la tâche pour laquelle on souhaite connaître les totaux.
	 * @param fromDate
	 *            date de départ à prendre en compte pour le calcul.
	 * @param toDate
	 *            date de fin à prendre en compte pour le calcul.
	 * @return les totaux associés à une tache (consommé, etc.).
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	TaskSums getTaskSums(Task task, Calendar fromDate, Calendar toDate)
			throws DAOException;

}
