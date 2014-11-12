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
 * ON ANY THEORY OF LIABILITY, WHETHER IN ContributionRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.core;

import java.io.InputStream;
import java.util.Calendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;

/**
 * Composant offrant les services de base de persistence de l'application.
 */
public interface ICoreDAO {

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean tablesExist() throws DAOException;

	/**
	 * Vérifie si une table existe dans le modèle.
	 * 
	 * @param tableName
	 *            le nom de la table.
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean tableExists(String tableName) throws DAOException;

	/**
	 * Crée les tables du modèle de données.
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void createTables() throws DAOException;

	/**
	 * Executes a SQL script.
	 * 
	 * @param scriptContent
	 *            the script content.
	 * @throws DAOException
	 *             thrown if a database error occurs.
	 */
	void executeScript(InputStream scriptContent) throws DAOException;

	/**
	 * Executes a SQL script.
	 * 
	 * @param scriptContent
	 *            the script content.
	 * @throws DAOException
	 *             thrown if a database error occurs.
	 */
	void executeScript(String scriptContent) throws DAOException;

	/**
	 * @param parentTaskId
	 *            the task identifier.
	 * @return the sub tasks count.
	 * @throws DAOException
	 *             thrown if a DAO exception occurs.
	 */
	int getSubTasksCount(long parentTaskId) throws DAOException;

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
