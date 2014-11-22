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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.impl.dao.CoreDAOImpl;
import org.xml.sax.SAXException;

/**
 * Model manager.
 */
public interface IModelMgr {

	/**
	 * Change la tache d'une liste de contributions.
	 * 
	 * @param contributions
	 *            la liste de contributions.
	 * @param newContributionTask
	 *            la tache à affecter.
	 * @return la liste de contributions mise à jour.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas où la tache cible ne peut être acdepter de
	 *             contribution.
	 * 
	 */
	Contribution[] changeContributionTask(Contribution[] contributions,
			Task newContributionTask) throws DAOException, ModelException;

	/**
	 * Vérifie si la tache spécifiée peut accueillir des sous-taches.
	 * 
	 * @param task
	 *            la tache à controler.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	void checkAcceptsSubtasks(Task task) throws DAOException,
			ModelException;

	/**
	 * Crée un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à créer.
	 * @return le collaborateur après création.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	Collaborator createCollaborator(Collaborator collaborator)
			throws DAOException, ModelException;

	/**
	 * Crée une contribution.
	 * 
	 * @param contribution
	 *            la contribution à créer.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être décrémenté.
	 * @return la contribution après création.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de contribution.
	 */
	Contribution createContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DAOException,
			ModelException;

	/**
	 * Crée une durée.
	 * 
	 * @param duration
	 *            la durée à créer.
	 * @return la durée créée.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la durée existe déjà.
	 */
	Duration createDuration(Duration duration) throws DAOException,
			ModelException;

	/**
	 * Crée un nouveau collaborateur en générant automatiquement ses attributs.
	 * 
	 * @return le nouveau collaborateur.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator createNewCollaborator() throws DAOException;

	/**
	 * Crée une nouvelle tache en générant un nom et un code.
	 * 
	 * <p>
	 * Avant création, les caractéristiques de la tache de destination sont
	 * controllées pour voir si elle peut accueillir des sous-taches.
	 * </p>
	 * 
	 * <p>
	 * Cette méthode est synchronisé en raison de la génération du numéro de la
	 * tache qui est déplacée à un autre chemin.
	 * </p>
	 * 
	 * @param parentTask
	 *            la tache parent de destination.
	 * @return la tache créée.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 * @see IModelMgr#checkAcceptsSubtasks(Task)
	 */
	Task createNewTask(Task parentTask) throws DAOException,
			ModelException;

	/**
	 * Crée les tables du modèle de données.
	 * 
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void createTables() throws DAOException;

	/**
	 * Crée une nouvelle tache.
	 * 
	 * <p>
	 * Avant création, les caractéristiques de la tache de destination sont
	 * controllées pour voir si elle peut accueillir des sous-taches.
	 * </p>
	 * 
	 * @param parentTask
	 *            la tache parent de destination.
	 * @param task
	 *            la tache à créer.
	 * @return la tache créée.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 * @see ImodelMgr#checkAcceptsSubtasks(Task)
	 */
	Task createTask(Task parentTask, Task task) throws DAOException,
			ModelException;

	/**
	 * Vérifie si la durée existe en base.
	 * 
	 * @param duration
	 *            la durée à vérifier.
	 * @return un booléen indiquant si la durée existe.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean durationExists(Duration duration) throws DAOException;

	/**
	 * Exporte le contenu de la base dans un fichier XML.
	 * 
	 * @param out
	 *            le flux dans lequel est généré le flux XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 * @throws DAOException
	 *             levé en cas d'incident avec la base de données.
	 */
	void exportToXML(OutputStream out) throws IOException, DAOException;

	/**
	 * Tells whether the given task is leaf or not.
	 * 
	 * @param taskId
	 *            the task identifier.
	 * @return <code>true</code> if the task is leaf.
	 * @throws DAOException
	 *             thrown if a DAO exception occurs.
	 */
	boolean isLeaf(long taskId) throws DAOException;

	/**
	 * @param parentTaskId
	 *            the task identifier.
	 * @return the sub tasks count.
	 * @throws DAOException
	 *             thrown if a DAO exception occurs.
	 */
	int getSubTasksCount(long parentTaskId) throws DAOException;

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs actifs.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator[] getActiveCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort) throws DAOException;

	/**
	 * @return la liste des durées actives.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration[] getActiveDurations() throws DAOException;

	/**
	 * @param collaboratorId
	 *            l'identifiant du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant est spécifié.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator getCollaborator(long collaboratorId) throws DAOException;

	/**
	 * @param login
	 *            l'identifiant de connexion du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant de connexion est spécifié.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator getCollaborator(String login) throws DAOException;

	/**
	 * @return la liste des collaborateurs.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator[] getCollaborators() throws DAOException;

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Collaborator[] getCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort) throws DAOException;

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
	Task[] getContributedTasks(Collaborator contributor,
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
	 * @return la liste des contributions associées aux paramétres spécifiés.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 */
	Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException,
			ModelException;

	/**
	 * Calcule le nombre des contributions associée aux paramétres spécifiés.
	 * 
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return le nombre de contributions.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données en entrée avec le
	 *             modèle.
	 * 
	 * @see CoreDAOImpl.tools.activitymgr.core.DbMgrImpl#getContributionsNb(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws ModelException,
			DAOException;

	/**
	 * Calcule le total des contributions associée aux paramétres spécifiés.
	 * 
	 * @param task
	 *            the task of the contributions to select (or parent task).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la seomme des contributions.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 */
	long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException,
			ModelException;

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
	 * @throws ModelException
	 *             levé si l'interval est incohérent.
	 */
	Collaborator[] getContributors(Task task, Calendar fromDate, Calendar toDate)
			throws DAOException, ModelException;

	/**
	 * @param durationId
	 *            identifiant de la durée.
	 * @return la durée dont l'identifiant est spécifiée.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration getDuration(long durationId) throws DAOException;

	/**
	 * @return la liste des durées actives.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration[] getDurations() throws DAOException;

	/**
	 * Retourne la liste des contributions associées à une tache, un
	 * collaborateur et à un interval de temps donnés.
	 * 
	 * <p>
	 * Un tableau dont la taille est égale au nombre de jours séparant les deux
	 * dates spécifiées est retourné.
	 * 
	 * @param contributor
	 *            le collaborateur associé aux contributions.
	 * @param task
	 *            la tache associée aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la liste des contributions.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la date de fin spécifiée est antérieure à
	 *             la date de début spécifiée.
	 */
	IntervalContributions getIntervalContributions(
			Collaborator contributor, Task task, Calendar fromDate,
			Calendar toDate) throws DAOException, ModelException;

	/**
	 * Retourne la liste des conteneurs des taches contribuées dans un
	 * intervalle de temps donné.
	 * <p>
	 * Cette méthode est utilisée pour alimenter les taches
	 * &quote;récentes&quote; dans le dialogue de choix de tache.
	 * </p>
	 * 
	 * @param contributor
	 *            le collaborateur associé aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la liste des conteneurs de tache.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la date de fin spécifiée est antérieure à
	 *             la date de début spécifiée.
	 */
	Task[] getContributedTaskContainers(Collaborator contributor,
			Calendar fromDate, Calendar toDate) throws DAOException,
			ModelException;
	
	/**
	 * @param task
	 *            la tache dont on veut connaitre la tache parent.
	 * @return la tache parent d'une tache spécifiée.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task getParentTask(Task task) throws DAOException;

	/**
	 * @return the root tasks count.
	 * @throws DAOException
	 *             thrown if a database exception occurs.
	 */
	int getRootTasksCount() throws DAOException;

	/**
	 * @param parentTaskId
	 *            l'identifiant de la tache dont on veut connaître les
	 *            sous-taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task[] getSubtasks(Long parentTaskId) throws DAOException;

	/**
	 * @param parentTask
	 *            la tache dont on veut connaître les sous-taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task[] getSubTasks(Task parentTask) throws DAOException;

	/**
	 * @param taskId
	 *            l'identifiant de la tache recherchée.
	 * @return la tache dont l'identifiant est spécifié.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task getTask(long taskId) throws DAOException;

	/**
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskCode
	 *            le code de la tache recherchée.
	 * @return la tache dont le code et la tache parent sont spécifiés.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Task getTask(String taskPath, String taskCode) throws DAOException;

	/**
	 * Retourne la tache associée à un chemin construit à partir de codes de
	 * taches.
	 * 
	 * @param codePath
	 *            le chemin à base de code.
	 * @return la tache trouvée.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin de tache est inconnu.
	 */
	Task getTaskByCodePath(final String codePath) throws DAOException,
			ModelException;

	/**
	 * Construit le chemin de la tâche à partir des codes de tache.
	 * 
	 * @param task
	 *            la tache dont on veut connaître le chemin.
	 * @return le chemin.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	String getTaskCodePath(Task task) throws ModelException, DAOException;

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
	Task[] getTasks(TaskSearchFilter filter) throws DAOException;

	/**
	 * Retourne la liste des taches associées aux chemins spécifiés.
	 * 
	 * @param codePaths
	 *            la liste des chemins.
	 * @return la liste des tâches.
	 * @throws DAOException
	 * @throws ModelException
	 *             levé dans le cas ou une tache n'existe pas.
	 */
	Task[] getTasksByCodePath(String[] codePaths) throws DAOException,
			ModelException;

	/**
	 * @param task
	 *            la tâche pour laquelle on souhaite connaître les totaux.
	 * @param fromDate
	 *            date de départ à prendre en compte pour le calcul.
	 * @param toDate
	 *            date de fin à prendre en compte pour le calcul.
	 * @return les totaux associés à une tache (consommé, etc.).
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache en
	 *             base ne sont pas ceux de la tache spécifiée.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	TaskSums getTaskSums(Task task, Calendar fromDate, Calendar toDate)
			throws ModelException, DAOException;

	/**
	 * Importe le contenu d'un fichier XML.
	 * 
	 * @param in
	 *            le flux depuis lequel est lu le flux XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de la lecture sur le flux
	 *             d'entrée
	 * @throws DAOException
	 *             levé en cas d'incident avec la base de données.
	 * @throws ParserConfigurationException
	 *             levé en cas de mauvaise configuration du parser XML.
	 * @throws SAXException
	 *             levé en cas d'erreur de mauvais format du fichier XML.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données lors de l'import
	 */
	void importFromXML(InputStream in) throws IOException, DAOException,
			ParserConfigurationException, SAXException, ModelException;

	/**
	 * Déplace la tache d'un cran vers le bas.
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * @param task
	 *            la tache à déplacer vers le bas.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	void moveDownTask(Task task) throws ModelException, DAOException;

	/**
	 * Déplace la tache vers un autre endroit dans la hiérarchie des taches.
	 * 
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * <p>
	 * Cette méthode est synchronisé en raison de la génération du numéro de la
	 * tache qui est déplacée à un autre chemin.
	 * </p>
	 * 
	 * @param task
	 *            la tache à déplacer.
	 * @param destParentTask
	 *            tache parent de destination.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	void moveTask(Task task, Task destParentTask) throws ModelException,
			DAOException;

	/**
	 * Déplace une tache de plus d'un cran (au contraire des méthodes
	 * <code>moveUp</code> et <code>moveDown</code>.
	 * 
	 * @param task
	 *            la tache à déplacer.
	 * @param newTaskNumber
	 *            le nouveau numéro de la tâche.
	 * @throws ModelException
	 *             levé en cas de violation du modèle.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	void moveTaskUpOrDown(Task task, int newTaskNumber)
			throws ModelException, DAOException;

	/**
	 * Déplace la tache d'un cran vers le haut.
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * @param task
	 *            la tache à déplacer vers le haut.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	void moveUpTask(Task task) throws ModelException, DAOException;

	/**
	 * Supprime un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à supprimer.
	 * @throws ModelException
	 *             levé dans le cas ou le collaborateur est associé à des
	 *             contributions en base.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void removeCollaborator(Collaborator collaborator)
			throws ModelException, DAOException;

	/**
	 * Supprime une contribution.
	 * 
	 * @param contribution
	 *            la contribution à supprimer.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être incrémenté.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la donnée a changé en base de données.
	 */
	void removeContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DAOException,
			ModelException;

	/**
	 * Supprime des contributions.
	 * 
	 * @param contributions
	 *            les contributions à supprimer.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void removeContributions(Contribution[] contributions)
			throws DAOException;

	/**
	 * Supprime une durée du référentiel de durées.
	 * 
	 * @param duration
	 *            la durée à supprimer.
	 * @throws ModelException
	 *             levé dans le cas ou la durée n'existe pas en base.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	void removeDuration(Duration duration) throws ModelException,
			DAOException;

	/**
	 * Supprime une tache.
	 * 
	 * <p>
	 * Cette méthode est synchronisé en raison de la modification potentielle du
	 * numéro de certaines taches.
	 * </p>
	 * 
	 * @param task
	 *            la tache à supprimer.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas de violation d'une contrainte d'intégrité du
	 *             modèle.
	 */
	void removeTask(Task task) throws DAOException, ModelException;

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	boolean tablesExist() throws DAOException;

	/**
	 * Modifie les attributs d'un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à modifier.
	 * @return le collaborateur modifié.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas de non unicité du login.
	 */
	Collaborator updateCollaborator(Collaborator collaborator)
			throws DAOException, ModelException;

	/**
	 * Modifie les attributs d'une contribution.
	 * 
	 * @param contribution
	 *            la contribution à modifier.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être décrémenté.
	 * @return la contribution modifiée.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la donnée a changé en base de données.
	 */
	Contribution updateContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DAOException,
			ModelException;

	/**
	 * Met à jour une durée.
	 * 
	 * @param duration
	 *            la durée à mettre à jour.
	 * @return la durée mise à jour.
	 * @throws DAOException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	Duration updateDuration(Duration duration) throws DAOException;

	/**
	 * Modifie une durée.
	 * <p>
	 * Pour pouvoir être modifiée, la durée ne doit pas être utilisée.
	 * </p>
	 * 
	 * @param duration
	 *            la durée à modifier.
	 * @param newDuration
	 *            la nouvelle valeur de la durée.
	 * @return la durée modifiée.
	 * @throws ModelException
	 *             levé dans le cas ou la durée à changer est utilisée ou dans
	 *             le cas ou la nouvelle valeur pour la durée existe déja dans
	 *             le référentiel.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	Duration updateDuration(Duration duration, Duration newDuration)
			throws ModelException, DAOException;

	/**
	 * Met à jour les attributs d'une tache en base.
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * @param task
	 *            la tache à mettre à jour.
	 * @return la tache mise à jour.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 * @throws DAOException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	Task updateTask(Task task) throws ModelException, DAOException;
}
