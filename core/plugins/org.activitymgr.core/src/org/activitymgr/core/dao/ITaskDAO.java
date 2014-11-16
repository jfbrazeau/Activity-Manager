package org.activitymgr.core.dao;

import java.util.Calendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;

public interface ITaskDAO extends IDAO<Task> {

	/**
	 * @param parentTaskId
	 *            the task identifier.
	 * @return the sub tasks count.
	 * @throws DAOException
	 *             thrown if a DAO exception occurs.
	 */
	int getSubTasksCount(long parentTaskId) throws DAOException;

	/**
	 * Retourn la liste des taches correspondant au filtre de recherche
	 * spécifié.
	 * 
	 * @param filter
	 *            le filtre de recherche.
	 * @return la liste des taches correspondant au filtre de recherche
	 *         spécifié.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	long[] getTaskIds(TaskSearchFilter filter) throws DAOException;

	/**
	 * @param contributor
	 *            le contributeur.
	 * @param fromDate
	 *            date de début.
	 * @param toDate
	 *            date de fin.
	 * @return la liste de taches associées au collaborateur entre les 2 dates
	 *         spécifiées.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	long[] getContributedTaskIds(Collaborator contributor, Calendar fromDate,
			Calendar toDate) throws DAOException;

	/**
	 * Génère un nouveau numéro de tache pour un chemin donné.
	 * 
	 * @param path
	 *            le chemin considéré.
	 * @return le numéro généré.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	byte newTaskNumber(String path) throws DAOException;

}
