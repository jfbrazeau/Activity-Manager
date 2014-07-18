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
import org.xml.sax.SAXException;

/**
 * Model manager.
 */
public interface IModelMgr {

	/**
	 * Initializes the database.
	 * 
	 * @throws DbException
	 *             thrown if a database exception occurs.
	 */
	public void initialize() throws DbException;

	/**
	 * Change la tache d'une liste de contributions.
	 * 
	 * @param contributions
	 *            la liste de contributions.
	 * @param newContributionTask
	 *            la tache à affecter.
	 * @return la liste de contributions mise à jour.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas où la tache cible ne peut être acdepter de
	 *             contribution.
	 * 
	 */
	public Contribution[] changeContributionTask(Contribution[] contributions,
			Task newContributionTask) throws DbException, ModelException;

	/**
	 * Vérifie si la tache spécifiée peut accueillir des sous-taches.
	 * 
	 * @param task
	 *            la tache à controler.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	public void checkAcceptsSubtasks(Task task) throws DbException,
			ModelException;

	/**
	 * Crée un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à créer.
	 * @return le collaborateur après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	public Collaborator createCollaborator(Collaborator collaborator)
			throws DbException, ModelException;

	/**
	 * Crée une contribution.
	 * 
	 * @param contribution
	 *            la contribution à créer.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être décrémenté.
	 * @return la contribution après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de contribution.
	 */
	public Contribution createContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DbException,
			ModelException;

	/**
	 * Crée une durée.
	 * 
	 * @param duration
	 *            la durée à créer.
	 * @return la durée créée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la durée existe déjà.
	 */
	public Duration createDuration(Duration duration) throws DbException,
			ModelException;

	/**
	 * Crée un nouveau collaborateur en générant automatiquement ses attributs.
	 * 
	 * @return le nouveau collaborateur.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Collaborator createNewCollaborator() throws DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 * @see IModelMgr#checkAcceptsSubtasks(Task)
	 */
	public Task createNewTask(Task parentTask) throws DbException,
			ModelException;

	/**
	 * Crée les tables du modèle de données.
	 * 
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public void createTables() throws DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 * @see ImodelMgr#checkAcceptsSubtasks(Task)
	 */
	public Task createTask(Task parentTask, Task task) throws DbException,
			ModelException;

	/**
	 * Vérifie si la durée existe en base.
	 * 
	 * @param duration
	 *            la durée à vérifier.
	 * @return un booléen indiquant si la durée existe.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public boolean durationExists(Duration duration) throws DbException;

	/**
	 * Exporte le contenu de la base dans un fichier XML.
	 * 
	 * @param out
	 *            le flux dans lequel est généré le flux XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 * @throws DbException
	 *             levé en cas d'incident avec la base de données.
	 */
	public void exportToXML(OutputStream out) throws IOException, DbException;

	/**
	 * Tells whether the given task is leaf or not.
	 * 
	 * @param taskId
	 *            the task identifier.
	 * @return <code>true</code> if the task is leaf.
	 * @throws DbException
	 *             thrown if a DAO exception occurs.
	 */
	boolean isLeaf(long taskId) throws DbException;

	/**
	 * @param parentTaskId
	 *            the task identifier.
	 * @return the sub tasks count.
	 * @throws DbException
	 *             thrown if a DAO exception occurs.
	 */
	int getSubTasksCount(long parentTaskId) throws DbException;

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs actifs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Collaborator[] getActiveCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort) throws DbException;

	/**
	 * @return la liste des durées actives.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Duration[] getActiveDurations() throws DbException;

	/**
	 * @param collaboratorId
	 *            l'identifiant du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Collaborator getCollaborator(long collaboratorId) throws DbException;

	/**
	 * @param login
	 *            l'identifiant de connexion du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant de connexion est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Collaborator getCollaborator(String login) throws DbException;

	/**
	 * @return la liste des collaborateurs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Collaborator[] getCollaborators() throws DbException;

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Collaborator[] getCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort) throws DbException;

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
	public Task[] getContributedTasks(Collaborator contributor,
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
	 * @return la liste des contributions associées aux paramétres spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 */
	public Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException,
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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données en entrée avec le
	 *             modèle.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgrImpl#getContributionsNb(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	public int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws ModelException,
			DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 */
	public long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DbException,
			ModelException;

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
	 * @throws ModelException
	 *             levé si l'interval est incohérent.
	 */
	Collaborator[] getContributors(Task task, Calendar fromDate, Calendar toDate)
			throws DbException, ModelException;

	/**
	 * @param durationId
	 *            identifiant de la durée.
	 * @return la durée dont l'identifiant est spécifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Duration getDuration(long durationId) throws DbException;

	/**
	 * @return la liste des durées actives.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Duration[] getDurations() throws DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la date de fin spécifiée est antérieure à
	 *             la date de début spécifiée.
	 */
	public IntervalContributions getIntervalContributions(
			Collaborator contributor, Task task, Calendar fromDate,
			Calendar toDate) throws DbException, ModelException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la date de fin spécifiée est antérieure à
	 *             la date de début spécifiée.
	 */
	public Task[] getContributedTaskContainers(Collaborator contributor,
			Calendar fromDate, Calendar toDate) throws DbException,
			ModelException;
	
	/**
	 * @param task
	 *            la tache dont on veut connaitre la tache parent.
	 * @return la tache parent d'une tache spécifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Task getParentTask(Task task) throws DbException;

	/**
	 * @return the root tasks count.
	 * @throws DbException
	 *             thrown if a database exception occurs.
	 */
	int getRootTasksCount() throws DbException;

	/**
	 * @param parentTaskId
	 *            l'identifiant de la tache dont on veut connaître les
	 *            sous-taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Task[] getSubtasks(Long parentTaskId) throws DbException;

	/**
	 * @param parentTask
	 *            la tache dont on veut connaître les sous-taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Task[] getSubtasks(Task parentTask) throws DbException;

	/**
	 * @param taskId
	 *            l'identifiant de la tache recherchée.
	 * @return la tache dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Task getTask(long taskId) throws DbException;

	/**
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskCode
	 *            le code de la tache recherchée.
	 * @return la tache dont le code et la tache parent sont spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Task getTask(String taskPath, String taskCode) throws DbException;

	/**
	 * Retourne la tache associée à un chemin construit à partir de codes de
	 * taches.
	 * 
	 * @param codePath
	 *            le chemin à base de code.
	 * @return la tache trouvée.
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin de tache est inconnu.
	 */
	public Task getTaskByCodePath(final String codePath) throws DbException,
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
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	public String getTaskCodePath(Task task) throws ModelException, DbException;

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
	public Task[] getTasks(TaskSearchFilter filter) throws DbException;

	/**
	 * Retourne la liste des taches associées aux chemins spécifiés.
	 * 
	 * @param codePaths
	 *            la liste des chemins.
	 * @return la liste des tâches.
	 * @throws DbException
	 * @throws ModelException
	 *             levé dans le cas ou une tache n'existe pas.
	 */
	public Task[] getTasksByCodePath(String[] codePaths) throws DbException,
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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public TaskSums getTaskSums(Task task, Calendar fromDate, Calendar toDate)
			throws ModelException, DbException;

	/**
	 * Importe le contenu d'un fichier XML.
	 * 
	 * @param in
	 *            le flux depuis lequel est lu le flux XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de la lecture sur le flux
	 *             d'entrée
	 * @throws DbException
	 *             levé en cas d'incident avec la base de données.
	 * @throws ParserConfigurationException
	 *             levé en cas de mauvaise configuration du parser XML.
	 * @throws SAXException
	 *             levé en cas d'erreur de mauvais format du fichier XML.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données lors de l'import
	 */
	public void importFromXML(InputStream in) throws IOException, DbException,
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
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	public void moveDownTask(Task task) throws ModelException, DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	public void moveTask(Task task, Task destParentTask) throws ModelException,
			DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	public void moveTaskUpOrDown(Task task, int newTaskNumber)
			throws ModelException, DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	public void moveUpTask(Task task) throws ModelException, DbException;

	/**
	 * Supprime un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à supprimer.
	 * @throws ModelException
	 *             levé dans le cas ou le collaborateur est associé à des
	 *             contributions en base.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public void removeCollaborator(Collaborator collaborator)
			throws ModelException, DbException;

	/**
	 * Supprime une contribution.
	 * 
	 * @param contribution
	 *            la contribution à supprimer.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être incrémenté.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la donnée a changé en base de données.
	 */
	public void removeContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DbException,
			ModelException;

	/**
	 * Supprime des contributions.
	 * 
	 * @param contributions
	 *            les contributions à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public void removeContributions(Contribution[] contributions)
			throws DbException;

	/**
	 * Supprime une durée du référentiel de durées.
	 * 
	 * @param duration
	 *            la durée à supprimer.
	 * @throws ModelException
	 *             levé dans le cas ou la durée n'existe pas en base.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public void removeDuration(Duration duration) throws ModelException,
			DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas de violation d'une contrainte d'intégrité du
	 *             modèle.
	 */
	public void removeTask(Task task) throws DbException, ModelException;

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public boolean tablesExist() throws DbException;

	/**
	 * Modifie les attributs d'un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à modifier.
	 * @return le collaborateur modifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas de non unicité du login.
	 */
	public Collaborator updateCollaborator(Collaborator collaborator)
			throws DbException, ModelException;

	/**
	 * Modifie les attributs d'une contribution.
	 * 
	 * @param contribution
	 *            la contribution à modifier.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être décrémenté.
	 * @return la contribution modifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou la donnée a changé en base de données.
	 */
	public Contribution updateContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DbException,
			ModelException;

	/**
	 * Met à jour une durée.
	 * 
	 * @param duration
	 *            la durée à mettre à jour.
	 * @return la durée mise à jour.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public Duration updateDuration(Duration duration) throws DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	public Duration updateDuration(Duration duration, Duration newDuration)
			throws ModelException, DbException;

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
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	public Task updateTask(Task task) throws ModelException, DbException;
}
