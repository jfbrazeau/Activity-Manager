package org.activitymgr.core.dao;

import java.util.Calendar;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSearchFilter;
import org.activitymgr.core.dto.misc.TaskSums;

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
	 * Returns the sub tasks of a given task path filtered by a given string.
	 * <p>
	 * Note : the filter is not only applied on the sub tasks, it is also
	 * applied on any nested task in the tree. This helps to implement dialogs
	 * in which you can type a text and filter the resulting tree.
	 * </p>
	 * 
	 * @param taskPath
	 *            the task path.
	 * @param filter
	 *            a string that filters sub tasks.
	 * @return the sub task list.
	 */
	Task[] getSubTasks(String taskPath, String filter);

	/**
	 * Returns the first task matching the given filter.
	 * @param filter
	 *            a string that filters tasks.
	 * @return the matching task or null if no task matches.
	 */
	Task getFirstTaskMatching(String filter);

	
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

	/**
 	 * Compute the task sums for a given task (when
	 * <code>taskId</code> is specified) or a set of tasks specified by their
	 * path.
	 * 
	 * @param taskId
	 *            the task identifier for which we want to know the sums.
	 * @param tasksPath
	 *            the tasks path for which we want to know the sums.
	 * @return the sub tasks sums (budget, initially consumed, ...)
	 * @throws DAOException
	 *             thrown if a technical error occurs.
	 */
	List<TaskSums> getTasksSums(Long taskId, String tasksPath)
			throws DAOException;

}
