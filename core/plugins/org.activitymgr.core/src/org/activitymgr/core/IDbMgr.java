/*
 * Copyright (c) 2004-2012, Jean-Francois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.core;

import java.io.InputStream;
import java.util.Calendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;

/**
 * Composant offrant les services de base de persistence de l'application.
 */
public interface IDbMgr {

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean tablesExist() throws DbException;

	/**
	 * Vérifie si une table existe dans le modèle.
	 * 
	 * @param tableName
	 *            le nom de la table.
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean tableExists(String tableName) throws DbException;

	/**
	 * Crée les tables du modèle de données.
	 * 
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void createTables() throws DbException;

	/**
	 * Executes a SQL script.
	 * 
	 * @param scriptContent
	 *            the script content.
	 * @throws DbException
	 *             thrown if a database error occurs.
	 */
	void executeScript(InputStream scriptContent) throws DbException;

	/**
	 * Executes a SQL script.
	 * 
	 * @param scriptContent
	 *            the script content.
	 * @throws DbException
	 *             thrown if a database error occurs.
	 */
	void executeScript(String scriptContent) throws DbException;

	/**
	 * Crée un collaborateur.
	 * 
	 * @param newCollaborator
	 *            le collaborateur à créer.
	 * @return le collaborateur après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	<C extends Collaborator> C createCollaborator(C newCollaborator)
			throws DbException;

	/**
	 * Crée une contribution.
	 * 
	 * @param newContribution
	 *            la nouvelle contribution.
	 * @return la contribution après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Contribution createContribution(Contribution newContribution)
			throws DbException;

	/**
	 * Crée une contribution.
	 * 
	 * @param newDuration
	 *            la nouvelle durée.
	 * @return la durée après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration createDuration(Duration newDuration) throws DbException;

	/**
	 * Crée une tache.
	 * 
	 * <p>
	 * La tache parent peut être nulle pour indiquer que la nouvelle tache est
	 * une tache racine.
	 * </p>
	 * 
	 * @param parentTask
	 *            la tache parent accueillant la nouvelle tache.
	 * @param newTask
	 *            la nouvelle tache.
	 * @return la tache après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task createTask(Task parentTask, Task newTask) throws DbException;

	/**
	 * Vérifie si la durée est utilisée en base.
	 * 
	 * @param duration
	 *            la durée à vérifier.
	 * @return un booléen indiquant si la durée est utilisée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean durationIsUsed(Duration duration) throws DbException;

	/**
	 * @param parentTaskId
	 *            the task identifier.
	 * @return the sub tasks count.
	 * @throws DbException
	 *             thrown if a DAO exception occurs.
	 */
	int getSubTasksCount(long parentTaskId) throws DbException;

	/**
	 * @param collaboratorId
	 *            l'identifiant du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator getCollaborator(long collaboratorId) throws DbException;

	/**
	 * @param login
	 *            l'identifiant de connexion du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant de connexion est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator getCollaborator(String login) throws DbException;

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @param onlyActiveCollaborators
	 *            booléen indiquant si l'on ne doit retourner que les
	 *            collaborateurs actifs.
	 * @return la liste des collaborateurs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator[] getCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort, boolean onlyActiveCollaborators)
			throws DbException;

	/**
	 * @param task
	 *            la tache associée aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return the contributors list corresponding to the given date interval.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator[] getContributors(Task task, Calendar fromDate, Calendar toDate)
			throws DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException;

	/**
	 * @param durationId
	 *            l'identifiant de la durée.
	 * @return la durée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration getDuration(long durationId) throws DbException;

	/**
	 * @param onlyActiveDurations
	 *            booléen indiquant si l'on ne doit retourner que les durées
	 *            actives.
	 * @return la liste des durées.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration[] getDurations(boolean onlyActiveDurations) throws DbException;

	/**
	 * @param task
	 *            la tache dont on veut connaitre la tache parent.
	 * @return la tache parent d'une tache spécifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task getParentTask(Task task) throws DbException;

	/**
	 * @return the root tasks count.
	 * @throws DbException
	 *             thrown if a database exception occurs.
	 */
	int getRootTasksCount() throws DbException;

	/**
	 * @param path
	 *            le chemin dont on veut connaître les taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task[] getTasks(String path) throws DbException;

	/**
	 * @param parentTask
	 *            la tache parent dont on veut connaitre les sous-taches.
	 * @return la liste des sous-taches.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task[] getSubTasks(Task parentTask) throws DbException;

	/**
	 * Retourn la liste des taches correspondant au filtre de recherche
	 * spécifié.
	 * 
	 * @param filter
	 *            le filtre de recherche.
	 * @return la liste des taches correspondant au filtre de recherche
	 *         spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task[] getTasks(TaskSearchFilter filter) throws DbException;

	/**
	 * @param tasksIds
	 *            the task identifier.
	 * @return la tache dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task getTask(long taskId) throws DbException;

	/**
	 * @param tasksIds
	 *            the task identifiers list.
	 * @return the tasks.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task[] getTasks(long[] tasksIds) throws DbException;

	/**
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskNumber
	 *            le numéro de la tache recherchée.
	 * @return la tache dont le chemin et le numéro sont spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task getTask(String taskPath, byte taskNumber) throws DbException;

	/**
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskCode
	 *            le code de la tache recherchée.
	 * @return la tache dont le code et la tache parent sont spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task getTask(String taskPath, String taskCode) throws DbException;

	/**
	 * @param contributor
	 *            le contributeur.
	 * @param fromDate
	 *            date de début.
	 * @param toDate
	 *            date de fin.
	 * @return la liste de taches associées au collaborateur entre les 2 dates
	 *         spécifiées.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task[] getContributedTasks(Collaborator contributor, Calendar fromDate,
			Calendar toDate) throws DbException;

	/**
	 * @param task
	 *            la tâche pour laquelle on souhaite connaître les totaux.
	 * @param fromDate
	 *            date de départ à prendre en compte pour le calcul.
	 * @param toDate
	 *            date de fin à prendre en compte pour le calcul.
	 * @return les totaux associés à une tache (consommé, etc.).
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	TaskSums getTaskSums(Task task, Calendar fromDate, Calendar toDate)
			throws DbException;

	/**
	 * Supprime un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void removeCollaborator(Collaborator collaborator) throws DbException;

	/**
	 * Supprime une contribution.
	 * 
	 * @param contribution
	 *            la contribution à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void removeContribution(Contribution contribution) throws DbException;

	/**
	 * Supprime une durée du référentiel de durées.
	 * 
	 * @param duration
	 *            la durée à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void removeDuration(Duration duration) throws DbException;

	/**
	 * Supprime une tache.
	 * 
	 * @param task
	 *            la tache à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void removeTask(Task task) throws DbException;

	/**
	 * Modifie les attributs d'un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à modifier.
	 * @return le collaborateur modifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator updateCollaborator(Collaborator collaborator)
			throws DbException;

	/**
	 * Modifie les attributs d'une contribution.
	 * 
	 * @param contribution
	 *            la contribution à modifier.
	 * @return la contribution modifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Contribution updateContribution(Contribution contribution)
			throws DbException;

	/**
	 * Met à jour une durée.
	 * 
	 * @param duration
	 *            la durée à mettre à jour.
	 * @return la durée mise à jour.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration updateDuration(Duration duration) throws DbException;

	/**
	 * Modifie les attributs d'une tache.
	 * 
	 * @param task
	 *            la tache à modifier.
	 * @return la tache modifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task updateTask(Task task) throws DbException;

	/**
	 * Génère un nouveau numéro de tache pour un chemin donné.
	 * 
	 * @param path
	 *            le chemin considéré.
	 * @return le numéro généré.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	byte newTaskNumber(String path) throws DbException;

}
