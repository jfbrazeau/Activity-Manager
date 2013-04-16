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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.core.util.XmlHelper;
import org.activitymgr.core.util.XmlHelper.ModelMgrDelegate;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Gestionnaire du modèle.
 * 
 * <p>
 * Les services offerts par cette classe garantissent l'intégrité du modèle.
 * </p>
 */
public class ModelMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(ModelMgr.class);

	/**
	 * Initialise la connexion à la base de données.
	 * 
	 * @param driverName
	 *            le nom du driver JDBC.
	 * @param url
	 *            l'URL de connexion au serveur.
	 * @param user
	 *            l'identifiant de connexion/
	 * @param password
	 *            le mot de passe de connexion.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static void initDatabaseAccess(String driverName, String url,
			String user, String password) throws DbException {
		log.info("initDatabaseAccess(" + driverName + ", " + url + ", " + user + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		DbMgr.initDatabaseAccess(driverName, url, user, password);
	}

	/**
	 * Ferme la base de données.
	 * 
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la BDD.
	 */
	public static void closeDatabaseAccess() throws DbException {
		DbMgr.closeDatabaseAccess();
	}

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static boolean tablesExist() throws DbException {
		log.info("tablesExist()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Test d'existence des tables
			boolean tablesExist = DbMgr.tablesExist(tx);

			// Commit et fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return tablesExist;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Crée les tables du modèle de données.
	 * 
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static void createTables() throws DbException {
		log.info("createTables()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Test d'existence des tables
			DbMgr.createTables(tx);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Substitue une partie du chemin d'un groupe de tache et de leurs
	 * sous-taches par un nouvelle valeur.
	 * <p>
	 * Cette méthode est utilisée pour déplacer les sous-taches d'une tache qui
	 * vient d'être déplacée.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param tasks
	 *            les taches dont on veut changer le chemin.
	 * @param oldPathLength
	 *            la taille de la portion de chemin à changer.
	 * @param newPath
	 *            le nouveau chemin.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private static void changeTasksPaths(DbTransaction tx, Task[] tasks,
			int oldPathLength, String newPath) throws DbException {
		// Récupération de la liste des taches
		Iterator<Task> it = Arrays.asList(tasks).iterator();
		int newPathLength = newPath.length();
		StringBuffer buf = new StringBuffer(newPath);
		while (it.hasNext()) {
			Task task = it.next();
			log.debug("Updating path of task '" + task.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			// Mise à jour des taches filles
			Task[] subTasks = DbMgr.getSubtasks(tx, task);
			if (subTasks.length > 0)
				changeTasksPaths(tx, subTasks, oldPathLength, newPath);
			// Puis mise à jour de la tache elle-même
			buf.setLength(newPathLength);
			buf.append(task.getPath().substring(oldPathLength));
			log.debug(" - old path : '" + task.getPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			task.setPath(buf.toString());
			log.debug(" - new path : '" + task.getPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			// Mise à jour
			DbMgr.updateTask(tx, task);
		}
	}

	/**
	 * Vérifie si la tache spécifiée peut accueillir des sous-taches.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tache à controler.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	private static void checkAcceptsSubtasks(DbTransaction tx, Task task)
			throws DbException, ModelException {
		// Rafraichissement des attributs de la tache
		task = DbMgr.getTask(tx, task.getId());
		// Une tâche qui admet déja des sous-taches peut en admettre d'autres
		// La suite des controles n'est donc exécutée que si la tache n'admet
		// pas de sous-tâches
		if (task.getSubTasksCount() == 0) {
			// Une tache ne peut admettre une sous-tache que si elle
			// n'est pas déja associée à un consommé (ie: à des contributions)
			long contribsNb = DbMgr.getContributionsNb(tx, task, null, null,
					null, null);
			if (contribsNb != 0)
				throw new ModelException(
						Strings.getString(
								"ModelMgr.errors.TASK_USED_BY_CONTRIBUTIONS", task.getName(), new Long(contribsNb))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (task.getBudget() != 0)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.NON_NULL_TASK_BUDGET")); //$NON-NLS-1$
			if (task.getInitiallyConsumed() != 0)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.NON_NULL_TASK_INITIALLY_CONSUMMED")); //$NON-NLS-1$
			if (task.getTodo() != 0)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.NON_NULL_TASK_ESTIMATED_TIME_TO_COMPLETE")); //$NON-NLS-1$
		}
	}

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
	public static void checkAcceptsSubtasks(Task task) throws DbException,
			ModelException {
		log.info("checkAcceptsSubtasks(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Une tache ne peut admettre une sous-tache que si elle
			// n'est pas déja associée à un consommé
			checkAcceptsSubtasks(tx, task);

			// Commit et fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Vérifie que le chemin et le numéro de la tache en base de données
	 * coincident avec la copie de la tache spécifiée.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la copie de la tache en mémoire.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	private static void checkTaskPathAndUpdateSubTasksCount(DbTransaction tx,
			Task task) throws ModelException, DbException {
		boolean noErrorOccured = false;
		Task _task = null;
		try {
			_task = DbMgr.getTask(tx, task.getId());
			if (_task == null)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.UNKNOWN_TASK")); //$NON-NLS-1$
			if (!_task.getPath().equals(task.getPath()))
				throw new ModelException(
						Strings.getString("ModelMgr.errors.TASK_PATH_UPDATE_DETECTED")); //$NON-NLS-1$
			if (_task.getNumber() != task.getNumber())
				throw new ModelException(
						Strings.getString("ModelMgr.errors.TASK_NUMBER_UPDATE_DETECTED")); //$NON-NLS-1$
			task.setSubTasksCount(_task.getSubTasksCount());
			// Si aucune erreur n'est intervenue...
			noErrorOccured = true;
		} finally {
			if (!noErrorOccured && _task != null && task != null) {
				log.error("Task id = " + task.getId()); //$NON-NLS-1$
				log.error("     name = " + task.getName()); //$NON-NLS-1$
				log.error("     fullath = " + task.getPath() + "/" + task.getNumber()); //$NON-NLS-1$ //$NON-NLS-2$
				log.error("     db fullath = " + _task.getPath() + "/" + _task.getNumber()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Vérifie l'unicité d'un login.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param collaborator
	 *            le collaborateur dont on veut vérifier l'unicité de login.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans le cas ou le ogin n'est pas unique.
	 */
	private static void checkUniqueLogin(DbTransaction tx,
			Collaborator collaborator) throws DbException, ModelException {
		// Vérification de l'unicité
		Collaborator colWithSameLogin = DbMgr.getCollaborator(tx,
				collaborator.getLogin());
		// Vérification du login
		if (colWithSameLogin != null && !colWithSameLogin.equals(collaborator))
			throw new ModelException(
					Strings.getString(
							"ModelMgr.errors.NON_UNIQUE_COLLABORATOR_LOGIN", colWithSameLogin.getLogin())); //$NON-NLS-1$ //$NON-NLS-2$
	}

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
	public static Collaborator createCollaborator(Collaborator collaborator)
			throws DbException, ModelException {
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Création du collaborateur
			collaborator = createCollaborator(tx, collaborator);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return collaborator;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Crée un collaborateur dans un contexte de transaction.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param collaborator
	 *            le collaborateur à créer.
	 * @return le collaborateur après création.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	private static Collaborator createCollaborator(DbTransaction tx,
			Collaborator collaborator) throws DbException, ModelException {
		log.info("createCollaborator(" + collaborator + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Control de l'unicité du login
		checkUniqueLogin(tx, collaborator);

		// Création du collaborateur
		collaborator = DbMgr.createCollaborator(tx, collaborator);

		// Retour du résultat
		return collaborator;
	}

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
	public static Contribution createContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DbException,
			ModelException {
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Création de la contribution
			contribution = createContribution(tx, contribution,
					updateEstimatedTimeToComlete);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return contribution;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Crée une contribution dans un contexte de transaction.
	 * 
	 * @param tx
	 *            le contexte de transaction.
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
	private static Contribution createContribution(DbTransaction tx,
			Contribution contribution, boolean updateEstimatedTimeToComlete)
			throws DbException, ModelException {
		log.info("createContribution(" + contribution + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// La tache ne peut accepter une contribution que
		// si elle n'admet aucune sous-tache
		Task task = DbMgr.getTask(tx, contribution.getTaskId());
		if (task.getSubTasksCount() > 0)
			throw new ModelException(
					Strings.getString("ModelMgr.errors.TASK_WITH_AT_LEAST_ONE_SUBTASK_CANNOT_ACCEPT_CONTRIBUTIONS")); //$NON-NLS-1$

		// La durée existe-t-elle ?
		if (DbMgr.getDuration(tx, contribution.getDurationId()) == null) {
			throw new ModelException(
					Strings.getString("ModelMgr.errors.INVALID_DURATION")); //$NON-NLS-1$
		}

		// Création de la contribution
		contribution = DbMgr.createContribution(tx, contribution);

		// Faut-il mettre à jour automatiquement le RAF de la tache ?
		if (updateEstimatedTimeToComlete) {
			// Mise à jour du RAF de la tache
			long newEtc = task.getTodo() - contribution.getDurationId();
			task.setTodo(newEtc > 0 ? newEtc : 0);
			DbMgr.updateTask(tx, task);
		}

		// Retour du résultat
		return contribution;
	}

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
	public static Duration createDuration(Duration duration)
			throws DbException, ModelException {
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Création
			duration = createDuration(tx, duration);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return duration;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Crée une durée dans un contexte de transaction.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la durée à créer.
	 * @return la durée créée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé dans la cas ou la durée existe déjà.
	 */
	private static Duration createDuration(DbTransaction tx, Duration duration)
			throws DbException, ModelException {
		log.info("createDuration(" + duration + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Vérification de l'unicité
		if (durationExists(tx, duration))
			throw new ModelException(
					Strings.getString("ModelMgr.errros.DUPLICATE_DURATION")); //$NON-NLS-1$

		// Vérification de la non nullité
		if (duration.getId() == 0)
			throw new ModelException(
					Strings.getString("ModelMgr.errors.NUL_DURATION_FORBIDDEN")); //$NON-NLS-1$

		// Création
		duration = DbMgr.createDuration(tx, duration);

		// Retour du résultat
		return duration;
	}

	/**
	 * Crée un nouveau collaborateur en générant automatiquement ses attributs.
	 * 
	 * @return le nouveau collaborateur.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Collaborator createNewCollaborator() throws DbException {
		log.info("createNewCollaborator()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Le login doit être unique => il faut vérifier si
			// celui-ci n'a pas déja été attribué
			int idx = 0;
			boolean unique = false;
			String newLogin = null;
			while (!unique) {
				newLogin = "<" + Strings.getString("ModelMgr.defaults.COLLABORATOR_LOGIN_PREFIX") + (idx == 0 ? "" : String.valueOf(idx)) + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				unique = DbMgr.getCollaborator(tx, newLogin) == null;
				idx++;
			}
			// Création du nouveau collaborateur
			Collaborator collaborator = new Collaborator();
			collaborator.setLogin(newLogin);
			collaborator
					.setFirstName("<" + Strings.getString("ModelMgr.defaults.COLLABORATOR_FIRST_NAME") + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			collaborator
					.setLastName("<" + Strings.getString("ModelMgr.defaults.COLLABORATOR_LAST_NAME") + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			// Création en base
			collaborator = DbMgr.createCollaborator(tx, collaborator);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return collaborator;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	 * @see jfb.tools.activitymgr.core.ModelMgr#checkAcceptsSubtasks(Task)
	 */
	public static synchronized Task createNewTask(Task parentTask)
			throws DbException, ModelException {
		log.info("createNewTask(" + parentTask + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Le code doit être unique => il faut vérifier si
			// celui-ci n'a pas déja été attribué
			int idx = 0;
			boolean unique = false;
			String newCode = null;
			String taskPath = parentTask != null ? parentTask.getFullPath()
					: ""; //$NON-NLS-1$
			while (!unique) {
				newCode = "<N" + (idx == 0 ? "" : String.valueOf(idx)) + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				unique = DbMgr.getTask(tx, taskPath, newCode) == null;
				idx++;
			}
			// Création du nouveau collaborateur
			Task task = new Task();
			task.setName("<" + Strings.getString("ModelMgr.defaults.TASK_NAME") + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			task.setCode(newCode);

			// Création en base
			task = createTask(tx, parentTask, task);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return task;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Crée une nouvelle tache dans un contexte de transaction.
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
	 * @param tx
	 *            le contexte de transaction.
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
	 * @see jfb.tools.activitymgr.core.ModelMgr#checkAcceptsSubtasks(Task)
	 */
	private synchronized static Task createTask(DbTransaction tx,
			Task parentTask, Task task) throws DbException, ModelException {
		log.info("createTask(" + parentTask + ", " + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// Une tache ne peut admettre une sous-tache que si elle
		// n'est pas déja associée à un consommé
		if (parentTask != null)
			checkAcceptsSubtasks(tx, parentTask);

		// Check sur l'unicité du code pour le chemin considéré
		Task sameCodeTask = DbMgr
				.getTask(
						tx,
						parentTask != null ? parentTask.getFullPath() : "", task.getCode()); //$NON-NLS-1$
		if (sameCodeTask != null && !sameCodeTask.equals(task))
			throw new ModelException(
					Strings.getString("ModelMgr.errors.TASK_CODE_ALREADY_IN_USE")); //$NON-NLS-1$

		// Création de la tache
		task = DbMgr.createTask(tx, parentTask, task);

		// Retour du résultat
		return task;
	}

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
	 * @see jfb.tools.activitymgr.core.ModelMgr#checkAcceptsSubtasks(Task)
	 */
	public static Task createTask(Task parentTask, Task task)
			throws DbException, ModelException {
		log.info("createTask(" + parentTask + ", " + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Création de la tache
			task = createTask(tx, parentTask, task);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return task;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Vérifie si la durée existe en base.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la durée à vérifier.
	 * @return un booléen indiquant si la durée existe.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private static boolean durationExists(DbTransaction tx, Duration duration)
			throws DbException {
		boolean exists = (DbMgr.getDuration(tx, duration.getId()) != null);
		return exists;
	}

	/**
	 * Vérifie si la durée existe en base.
	 * 
	 * @param duration
	 *            la durée à vérifier.
	 * @return un booléen indiquant si la durée existe.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static boolean durationExists(Duration duration) throws DbException {
		log.info("durationExists(" + duration + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Calcul des sommes
			boolean exists = durationExists(tx, duration);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return exists;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static void importFromXML(InputStream in) throws IOException,
			DbException, ParserConfigurationException, SAXException,
			ModelException {
		log.info("importFromXML()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Création du gestionnaire de modèle de données
			ModelMgrDelegate modelMgrDelegate = new ModelMgrDelegate() {
				Map<String, Task> taskCache = new HashMap<String, Task>();
				Map<String, Collaborator> collaboratorsCache = new HashMap<String, Collaborator>();

				public Duration createDuration(DbTransaction tx,
						Duration duration) throws ModelException, DbException {
					return ModelMgr.createDuration(tx, duration);
				}

				public Collaborator createCollaborator(DbTransaction tx,
						Collaborator collaborator) throws DbException,
						ModelException {
					collaborator = ModelMgr
							.createCollaborator(tx, collaborator);
					collaboratorsCache.put(collaborator.getLogin(),
							collaborator);
					return collaborator;
				}

				public Task createTask(DbTransaction tx, Task parentTask,
						Task task) throws DbException, ModelException {
					task = ModelMgr.createTask(tx, parentTask, task);
					String taskPath = getTaskCodePath(tx, task);
					taskCache.put(taskPath, task);
					return task;
				}

				public Contribution createContribution(DbTransaction tx,
						Contribution contribution) throws DbException,
						ModelException {
					return ModelMgr.createContribution(tx, contribution, false);
				}

				public Task getTaskByCodePath(DbTransaction tx, String codePath)
						throws DbException, ModelException {
					Task task = (Task) taskCache.get(codePath);
					if (task == null) {
						task = ModelMgr.getTaskByCodePath(tx, codePath);
						taskCache.put(codePath, task);
					}
					return task;
				}

				public Collaborator getCollaborator(DbTransaction tx,
						String login) throws DbException {
					Collaborator collaborator = (Collaborator) collaboratorsCache
							.get(login);
					if (collaborator == null) {
						collaborator = DbMgr.getCollaborator(tx, login);
						collaboratorsCache.put(login, collaborator);
					}
					return collaborator;
				}
			};

			// Import des données
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			factory.setNamespaceAware(false);
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			XmlHelper xmlHelper = new XmlHelper(modelMgrDelegate, tx);
			// La DTD est chargée dans le CLASSPATH
			reader.setEntityResolver(xmlHelper);
			// Positionnement du gestionnaire d'erreur
			reader.setErrorHandler(xmlHelper);
			// Positionnement du gestionnaire de contenu XML
			reader.setContentHandler(xmlHelper);
			// Parsing du fichier
			InputSource is = new InputSource(in);
			is.setSystemId(""); // Pour empâcher la levée d'erreur associé à l'URI de la DTD //$NON-NLS-1$
			reader.parse(is);

			// Fermeture du flux de données
			in.close();
			in = null;

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} catch (SAXParseException e) {
			if (e.getCause() instanceof ModelException)
				throw (ModelException) e.getCause();
			else if (e.getCause() instanceof DbException)
				throw (DbException) e.getCause();
			else
				throw e;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static void exportToXML(OutputStream out) throws IOException,
			DbException {
		log.info("exportToXML()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Entête XML
			XmlHelper
					.println(out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			XmlHelper.println(out,
					"<!DOCTYPE model SYSTEM \"activitymgr.dtd\">"); //$NON-NLS-1$

			// Ajout des sommes de controle
			Task[] rootTasks = DbMgr.getSubtasks(tx, null);
			if (rootTasks.length > 0) {
				XmlHelper.println(out, "<!-- "); //$NON-NLS-1$
				XmlHelper
						.println(
								out,
								Strings.getString("ModelMgr.xmlexport.comment.ROOT_TASKS_CHECK_SUMS")); //$NON-NLS-1$
				for (int i = 0; i < rootTasks.length; i++) {
					Task rootTask = rootTasks[i];
					TaskSums sums = DbMgr.getTaskSums(tx, rootTask, null, null);
					XmlHelper
							.println(
									out,
									Strings.getString(
											"ModelMgr.xmlexport.comment.ROOT_TASK", new Integer(i), rootTask.getCode(), rootTask.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					XmlHelper
							.println(
									out,
									Strings.getString("ModelMgr.xmlexport.comment.BUDGET") + (sums.getBudgetSum() / 100d)); //$NON-NLS-1$
					XmlHelper
							.println(
									out,
									Strings.getString("ModelMgr.xmlexport.comment.INITIALLY_CONSUMED") + (sums.getInitiallyConsumedSum() / 100d)); //$NON-NLS-1$
					XmlHelper
							.println(
									out,
									Strings.getString("ModelMgr.xmlexport.comment.CONSUMED") + (sums.getConsumedSum() / 100d)); //$NON-NLS-1$
					XmlHelper
							.println(
									out,
									Strings.getString("ModelMgr.xmlexport.comment.ESTIMATED_TIME_TO_COMPLETE") + (sums.getTodoSum() / 100d)); //$NON-NLS-1$
					XmlHelper
							.println(
									out,
									Strings.getString("ModelMgr.xmlexport.comment.CONTRIBUTIONS_NUMBER") + sums.getContributionsNb()); //$NON-NLS-1$
				}
				XmlHelper.println(out, "  -->"); //$NON-NLS-1$
			}

			// Ajout du noeud racine
			XmlHelper.startXmlNode(out, "", XmlHelper.MODEL_NODE); //$NON-NLS-1$
			final String INDENT = "      "; //$NON-NLS-1$

			// Exportation des durées
			Duration[] durations = DbMgr.getDurations(tx, false);
			if (durations.length > 0) {
				XmlHelper.startXmlNode(out, "  ", XmlHelper.DURATIONS_NODE); //$NON-NLS-1$
				for (int i = 0; i < durations.length; i++) {
					Duration duration = durations[i];
					XmlHelper
							.startXmlNode(out, "    ", XmlHelper.DURATION_NODE); //$NON-NLS-1$
					XmlHelper.printTextNode(out, INDENT, XmlHelper.VALUE_NODE,
							String.valueOf(duration.getId()));
					XmlHelper.printTextNode(out, INDENT,
							XmlHelper.IS_ACTIVE_NODE,
							String.valueOf(duration.getIsActive()));
					XmlHelper.endXmlNode(out, "    ", XmlHelper.DURATION_NODE); //$NON-NLS-1$
				}
				XmlHelper.endXmlNode(out, "  ", XmlHelper.DURATIONS_NODE); //$NON-NLS-1$
			}
			// Exportation des collaborateurs
			Collaborator[] collaborators = DbMgr.getCollaborators(tx,
					Collaborator.LOGIN_FIELD_IDX, true, false);
			Map<Long, String> collaboratorsLoginsMap = new HashMap<Long, String>();
			if (collaborators.length > 0) {
				XmlHelper.startXmlNode(out, "  ", XmlHelper.COLLABORATORS_NODE); //$NON-NLS-1$
				for (int i = 0; i < collaborators.length; i++) {
					Collaborator collaborator = collaborators[i];
					// Enregitrement du login dans le dictionnaire de logins
					collaboratorsLoginsMap.put(new Long(collaborator.getId()),
							collaborator.getLogin());
					XmlHelper.startXmlNode(out,
							"    ", XmlHelper.COLLABORATOR_NODE); //$NON-NLS-1$
					XmlHelper.printTextNode(out, INDENT, XmlHelper.LOGIN_NODE,
							collaborator.getLogin());
					XmlHelper.printTextNode(out, INDENT,
							XmlHelper.FIRST_NAME_NODE,
							collaborator.getFirstName());
					XmlHelper.printTextNode(out, INDENT,
							XmlHelper.LAST_NAME_NODE,
							collaborator.getLastName());
					XmlHelper.printTextNode(out, INDENT,
							XmlHelper.IS_ACTIVE_NODE,
							String.valueOf(collaborator.getIsActive()));
					XmlHelper.endXmlNode(out,
							"    ", XmlHelper.COLLABORATOR_NODE); //$NON-NLS-1$
				}
				XmlHelper.endXmlNode(out, "  ", XmlHelper.COLLABORATORS_NODE); //$NON-NLS-1$
			}
			// Exportation des taches
			Map<Long, String> tasksCodePathMap = new HashMap<Long, String>();
			exportSubTasksToXML(tx, out, INDENT, null, "", tasksCodePathMap); //$NON-NLS-1$
			// Exportation des contributions
			Contribution[] contributions = DbMgr.getContributions(tx, null,
					null, null, null, null);
			if (contributions.length > 0) {
				XmlHelper.startXmlNode(out, "  ", XmlHelper.CONTRIBUTIONS_NODE); //$NON-NLS-1$
				for (int i = 0; i < contributions.length; i++) {
					Contribution contribution = contributions[i];
					XmlHelper.print(out, "    <"); //$NON-NLS-1$
					XmlHelper.print(out, XmlHelper.CONTRIBUTION_NODE);
					XmlHelper.printTextAttribute(out, XmlHelper.YEAR_ATTRIBUTE,
							String.valueOf(contribution.getYear()));
					XmlHelper.printTextAttribute(out,
							XmlHelper.MONTH_ATTRIBUTE,
							String.valueOf(contribution.getMonth()));
					XmlHelper.printTextAttribute(out, XmlHelper.DAY_ATTRIBUTE,
							String.valueOf(contribution.getDay()));
					XmlHelper.printTextAttribute(out,
							XmlHelper.DURATION_ATTRIBUTE,
							String.valueOf(contribution.getDurationId()));
					XmlHelper.println(out, ">"); //$NON-NLS-1$
					XmlHelper.printTextNode(out, INDENT,
							XmlHelper.CONTRIBUTOR_REF_NODE,
							(String) collaboratorsLoginsMap.get(new Long(
									contribution.getContributorId())));
					XmlHelper.printTextNode(out, INDENT,
							XmlHelper.TASK_REF_NODE, (String) tasksCodePathMap
									.get(new Long(contribution.getTaskId())));
					XmlHelper.endXmlNode(out,
							"    ", XmlHelper.CONTRIBUTION_NODE); //$NON-NLS-1$
				}
				XmlHelper.endXmlNode(out, "  ", XmlHelper.CONTRIBUTIONS_NODE); //$NON-NLS-1$
			}
			XmlHelper.endXmlNode(out, "", "model"); //$NON-NLS-1$ //$NON-NLS-2$
			out.flush();

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Ecrit les sous taches sous forme de XML dans le flux d'écriture.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param out
	 *            le flux d'écriture.
	 * @param indent
	 *            l'indentation.
	 * @param parentTask
	 *            la tache parent.
	 * @param parentCodePath
	 *            le chemin de la tache parente.
	 * @param taskCodesPathMap
	 *            cache contenant les taches indexées par leur chemin.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 * @throws DbException
	 *             levé en cas d'incident avec la base de données.
	 */
	private static void exportSubTasksToXML(DbTransaction tx, OutputStream out,
			String indent, Task parentTask, String parentCodePath,
			Map<Long, String> taskCodesPathMap) throws IOException, DbException {
		Task[] tasks = DbMgr.getSubtasks(tx, parentTask);
		if (tasks.length > 0) {
			// Cas particulier pour la racine
			if (parentTask == null)
				XmlHelper.startXmlNode(out, "  ", XmlHelper.TASKS_NODE); //$NON-NLS-1$
			for (int i = 0; i < tasks.length; i++) {
				Task task = tasks[i];
				XmlHelper.startXmlNode(out, "    ", XmlHelper.TASK_NODE); //$NON-NLS-1$
				String taskCodePath = parentCodePath + "/" + task.getCode(); //$NON-NLS-1$
				// Enregistrement du chemin dans le dictionnaire de chemins
				taskCodesPathMap.put(new Long(task.getId()), taskCodePath);
				XmlHelper.printTextNode(out, indent, XmlHelper.PATH_NODE,
						taskCodePath);
				XmlHelper.printTextNode(out, indent, XmlHelper.NAME_NODE,
						task.getName());
				XmlHelper.printTextNode(out, indent, XmlHelper.BUDGET_NODE,
						String.valueOf(task.getBudget()));
				XmlHelper.printTextNode(out, indent,
						XmlHelper.INITIALLY_CONSUMED_NODE,
						String.valueOf(task.getInitiallyConsumed()));
				XmlHelper.printTextNode(out, indent, XmlHelper.TODO_NODE,
						String.valueOf(task.getTodo()));
				if (task.getComment() != null)
					XmlHelper.printTextNode(out, indent,
							XmlHelper.COMMENT_NODE, task.getComment());
				XmlHelper.endXmlNode(out, "    ", XmlHelper.TASK_NODE); //$NON-NLS-1$
				if (task.getSubTasksCount() > 0) {
					exportSubTasksToXML(tx, out, indent, task, taskCodePath,
							taskCodesPathMap);
				}
			}
			// Cas particulier pour la racine
			if (parentTask == null)
				XmlHelper.endXmlNode(out, "  ", XmlHelper.TASKS_NODE); //$NON-NLS-1$
		}
	}

	/**
	 * @param collaboratorId
	 *            l'identifiant du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Collaborator getCollaborator(long collaboratorId)
			throws DbException {
		log.info("getCollaborator(" + collaboratorId + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des collaborateurs
			Collaborator collaborator = DbMgr.getCollaborator(tx,
					collaboratorId);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return collaborator;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @param login
	 *            l'identifiant de connexion du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant de connexion est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Collaborator getCollaborator(String login) throws DbException {
		log.info("getCollaborator(" + login + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des collaborateurs
			Collaborator collaborator = DbMgr.getCollaborator(tx, login);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return collaborator;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @return la liste des collaborateurs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Collaborator[] getCollaborators() throws DbException {
		return getCollaborators(Collaborator.LOGIN_FIELD_IDX, true, false);
	}

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs actifs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Collaborator[] getActiveCollaborators(
			int orderByClauseFieldIndex, boolean ascendantSort)
			throws DbException {
		return getCollaborators(orderByClauseFieldIndex, ascendantSort, true);
	}

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Collaborator[] getCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort) throws DbException {
		return getCollaborators(orderByClauseFieldIndex, ascendantSort, false);
	}

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
	private static Collaborator[] getCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort, boolean onlyActiveCollaborators)
			throws DbException {
		log.info("getCollaborators()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des collaborateurs
			Collaborator[] collaborators = DbMgr.getCollaborators(tx,
					orderByClauseFieldIndex, ascendantSort,
					onlyActiveCollaborators);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return collaborators;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Retourne les contributions associées aux paramétres spécifiés.
	 * 
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return les contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données en entrée avec le
	 *             modèle.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgr#getContributions(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	public static Contribution[] getContributions(Task task,
			Collaborator contributor, Integer year, Integer month, Integer day)
			throws ModelException, DbException {
		log.info("getContributions(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Vérification de la tache (le chemin de la tache doit être le bon
			// pour
			// que le calcul le soit)
			if (task != null)
				checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Récupération des durées
			Contribution[] result = DbMgr.getContributions(tx, task,
					contributor, year, month, day);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return result;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Calcule le nombre des contributions associée aux paramétres spécifiés.
	 * 
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données en entrée avec le
	 *             modèle.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgr#getContributionsNb(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	public static long getContributionsNb(Task task, Collaborator contributor,
			Integer year, Integer month, Integer day) throws ModelException,
			DbException {
		log.info("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération du résultat
			long sum = getContributionsNb(tx, task, contributor, year, month,
					day);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return sum;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	 * @deprecated
	 */
	public static long getContributionsSum(Task task, Collaborator contributor,
			Calendar fromDate, Calendar toDate) throws DbException {
		log.info("getContributionsSum(" + task + ", " + contributor + ", " + StringHelper.toYYYYMMDD(fromDate) + ", " + StringHelper.toYYYYMMDD(toDate) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération du résultat
			long sum = DbMgr.getContributionsSum(tx, task, contributor,
					fromDate, toDate);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return sum;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
			log.info("end of getContributionsSum");
		}
	}

	/**
	 * Calcule le cumul des consommations associees aux contributions associée
	 * pour les paramétres spécifiés.
	 * 
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données en entrée avec le
	 *             modèle.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgr#getContributionsSum(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 * @deprecated
	 */
	public static long getContributionsSum(Task task, Collaborator contributor,
			Integer year, Integer month, Integer day) throws ModelException,
			DbException {
		log.info("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération du résultat
			long sum = DbMgr.getContributionsSum(tx, task, contributor, year,
					month, day);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return sum;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
			log.info("end of getContributionsSum");
		}
	}

	/**
	 * Calcule le nombre de contributions associée aux paramétres spécifiés dans
	 * un contexte de transaction.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param year
	 *            l'année (facultative).
	 * @param month
	 *            le mois (facultatif).
	 * @param day
	 *            le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données en entrée avec le
	 *             modèle.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgr#getContributionsSum(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	public static long getContributionsNb(DbTransaction tx, Task task,
			Collaborator contributor, Integer year, Integer month, Integer day)
			throws ModelException, DbException {
		// Vérification de la tache (le chemin de la tache doit être le bon pour
		// que le calcul le soit)
		if (task != null)
			checkTaskPathAndUpdateSubTasksCount(tx, task);

		// Récupération du total
		long sum = DbMgr.getContributionsNb(tx, task, contributor, year, month,
				day);

		// Retour du résultat
		return sum;
	}

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
	 *            la tache associée aux contributions (optional).
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
	public static IntervalContributions getIntervalContributions(
			Collaborator contributor, Task task, Calendar fromDate,
			Calendar toDate) throws DbException, ModelException {
		log.info("getIntervalContributions(" + contributor + ", " + StringHelper.toYYYYMMDD(fromDate) + ", " + StringHelper.toYYYYMMDD(toDate) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Control sur la date
			if (fromDate.getTime().compareTo(toDate.getTime()) > 0)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.FROM_DATE_MUST_BE_BEFORE_TO_DATE")); //$NON-NLS-1$
			int daysCount = countDaysBetween(fromDate, toDate) + 1;
			
System.out.println("daysCount= "+ daysCount);
			// Récupération des contributions
			Contribution[] contributionsArray = DbMgr.getContributions(tx,
					contributor, task, fromDate, toDate);

			// Rangement des contributions par identifiant de tache
			// (as the tsk parameter can be omitted => in this case, several
			// tasks might be returned)
			Map<Long, TaskContributions> taskContributionsCache = new HashMap<Long, TaskContributions>();
			for (int i = 0; i < contributionsArray.length; i++) {
				Contribution contribution = contributionsArray[i];
				TaskContributions taskContributions = taskContributionsCache
						.get(contribution.getTaskId());
				// If the task contributions doesn't exist, we create it
				if (taskContributions == null) {
					taskContributions = new TaskContributions();
					taskContributions
							.setContributions(new Contribution[daysCount]);
					// Cache registering
					taskContributionsCache.put(contribution.getTaskId(),
							taskContributions);
				}
				Calendar contributionDate = new GregorianCalendar(
						contribution.getYear(), contribution.getMonth() - 1,
						contribution.getDay());
				int idx = countDaysBetween(fromDate, contributionDate);
				taskContributions.getContributions()[idx] = contribution;
			}

			// Task retrieval and sort
			long[] tasksIds = new long[taskContributionsCache.size()];
			int idx = 0;
			for (Long taskId : taskContributionsCache.keySet()) {
				tasksIds[idx++] = taskId;
			}
			Task[] tasks = DbMgr.getTasks(tx, tasksIds);
			Arrays.sort(tasks, new Comparator<Task>() {
				public int compare(Task t1, Task t2) {
					return t1.getFullPath().compareTo(t2.getFullPath());
				}
			});

			// Result building
			IntervalContributions result = new IntervalContributions();
			result.setFromDate(fromDate);
			result.setToDate(toDate);
			TaskContributions[] taskContributionsArray = new TaskContributions[tasks.length];
			result.setTaskContributions(taskContributionsArray);
			for (int i = 0; i < tasks.length; i++) {
				Task theTask = tasks[i];
				TaskContributions taskContributions = taskContributionsCache
						.get(theTask.getId());
				taskContributions.setTask(theTask);
				taskContributions.setTaskCodePath(getTaskCodePath(tx, theTask));
				taskContributionsArray[i] = taskContributions;
			}

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return result;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
			log.info("end of getIntervalContributions");
		}
	}

	/**
	 * @param date1
	 *            the first date.
	 * @param date2
	 *            the second date.
	 * @return the days count between the two dates.
	 */
	private static int countDaysBetween(Calendar date1, Calendar date2) {
		Calendar from = date1;
		Calendar to = date2;
		if (date1.after(date2)) {
			from = date2;
			to = date1;
		}
		int y1 = from.get(Calendar.YEAR);
		int y2 = to.get(Calendar.YEAR);
		// If both dates are within the same year, we only have to compare "day of year" fields
		if (y1 == y2) {
			return to.get(Calendar.DAY_OF_YEAR) - from.get(Calendar.DAY_OF_YEAR);
		}
		// In other cases, we have to increment a cursor to count how many days there is
		// between the current date and the 31th Dec. until the current year is equal to
		// the target date (because not all years have 365 days, some have 366!).
		else {
			int result = 0;
			Calendar fromClone = (Calendar) from.clone();
			while (fromClone.get(Calendar.YEAR) != y2) {
				// Save current day of year
				int dayOfYear = fromClone.get(Calendar.DAY_OF_YEAR);
				// Goto 31th of December
				fromClone.set(Calendar.MONTH, 11);
				fromClone.set(Calendar.DAY_OF_MONTH, 31);
				// Compute days count
				result += fromClone.get(Calendar.DAY_OF_YEAR) - dayOfYear;
				// Goto next year (= add one day)
				fromClone.add(Calendar.DATE, 1);
				result++;
			}
			// Compute last year days count
			result += to.get(Calendar.DAY_OF_YEAR) - fromClone.get(Calendar.DAY_OF_YEAR);
			return result;
		}
	}

	/**
	 * @return la liste des durées actives.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Duration[] getDurations() throws DbException {
		return getDurations(false);
	}

	/**
	 * @return la liste des durées actives.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Duration[] getActiveDurations() throws DbException {
		return getDurations(true);
	}

	/**
	 * @param onlyActiveCollaborators
	 *            booléen indiquant si l'on ne doit retourner que les
	 *            collaborateurs actifs.
	 * @return la liste des durées.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private static Duration[] getDurations(boolean onlyActiveCollaborators)
			throws DbException {
		log.info("getDurations()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des durées
			Duration[] durations = DbMgr.getDurations(tx,
					onlyActiveCollaborators);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return durations;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @param durationId
	 *            identifiant de la durée.
	 * @return la durée dont l'identifiant est spécifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Duration getDuration(long durationId) throws DbException {
		log.info("getDurations()"); //$NON-NLS-1$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des durées
			Duration duration = DbMgr.getDuration(tx, durationId);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return duration;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @param task
	 *            la tache dont on veut connaitre la tache parent.
	 * @return la tache parent d'une tache spécifiée.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Task getParentTask(Task task) throws DbException {
		log.info("getParentTask(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération de la tâche
			Task parentTask = DbMgr.getParentTask(tx, task);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return parentTask;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @param parentTaskId
	 *            l'identifiant de la tache dont on veut connaître les
	 *            sous-taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Task[] getSubtasks(Long parentTaskId) throws DbException {
		log.info("getSubtasks(" + parentTaskId + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des sous tâches
			Task parentTask = parentTaskId != null ? DbMgr.getTask(tx,
					parentTaskId) : null;
			Task[] subTasks = DbMgr.getSubtasks(tx, parentTask);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return subTasks;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @param parentTask
	 *            la tache dont on veut connaître les sous-taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Task[] getSubtasks(Task parentTask) throws DbException {
		log.info("getSubtasks(" + parentTask + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des sous tâches
			Task[] subTasks = DbMgr.getSubtasks(tx, parentTask);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return subTasks;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @param taskId
	 *            l'identifiant de la tache recherchée.
	 * @return la tache dont l'identifiant est spécifié.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Task getTask(long taskId) throws DbException {
		log.info("getTask(" + taskId + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération de la tâche
			Task task = DbMgr.getTask(tx, taskId);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return task;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static Task[] getTasks(TaskSearchFilter filter) throws DbException {
		log.info("getTasks(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération de la tâche
			Task[] tasks = DbMgr.getTasks(tx, filter);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return tasks;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskCode
	 *            le code de la tache recherchée.
	 * @return la tache dont le code et la tache parent sont spécifiés.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Task getTask(String taskPath, String taskCode)
			throws DbException {
		log.info("getTask(" + taskPath + ", " + taskCode + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération de la tâche
			Task task = DbMgr.getTask(tx, taskPath, taskCode);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return task;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static Task getTaskByCodePath(final String codePath)
			throws DbException, ModelException {
		log.info("getTaskByCodePath(" + codePath + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Recherche de la tache
			Task task = getTaskByCodePath(tx, codePath);

			// Retour du résultat
			return task;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Retourne la tache associée à un chemin construit à partir de codes de
	 * taches.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param codePath
	 *            le chemin à base de code.
	 * @return la tache trouvée.
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin de tache est inconnu.
	 */
	private static Task getTaskByCodePath(DbTransaction tx,
			final String codePath) throws DbException, ModelException {
		log.info("getTaskByCodePath(" + codePath + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!codePath.startsWith("/")) //$NON-NLS-1$
			throw new ModelException(
					Strings.getString("ModelMgr.errors.INVALID_TASK_CODE_PATH")); //$NON-NLS-1$
		// Recherche de la tache
		String subpath = codePath.trim().substring(1);
		log.debug("Processing task path '" + subpath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		Task task = null;
		while (subpath.length() > 0) {
			int idx = subpath.indexOf('/');
			String taskCode = idx >= 0 ? subpath.substring(0, idx) : subpath;
			String taskPath = task != null ? task.getFullPath() : ""; //$NON-NLS-1$
			subpath = idx >= 0 ? subpath.substring(idx + 1) : ""; //$NON-NLS-1$
			task = DbMgr.getTask(tx, taskPath, taskCode);
			if (task == null)
				throw new ModelException(Strings.getString(
						"ModelMgr.errors.UNKNOWN_TASK_CODE_PATH", codePath)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		log.debug("Found " + task); //$NON-NLS-1$

		// Retour du résultat
		return task;
	}

	/**
	 * @param collaborator
	 *            le collaborateur.
	 * @param fromDate
	 *            date de début.
	 * @param toDate
	 *            date de fin.
	 * @return la liste de taches associées au collaborateur entre les 2 dates
	 *         spécifiées.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Task[] getTasks(Collaborator collaborator, Calendar fromDate,
			Calendar toDate) throws DbException {
		log.info("getTasks(" + collaborator + ", " + StringHelper.toYYYYMMDD(fromDate) + ", " + StringHelper.toYYYYMMDD(toDate) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Récupération des tâches
			Task[] tasks = DbMgr.getTasks(tx, collaborator, fromDate, toDate);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return tasks;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static Task[] getTasksByCodePath(String[] codePaths)
			throws DbException, ModelException {
		log.info("getTasksByCodePath(" + codePaths + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Recherche des taches
			Task[] tasks = new Task[codePaths.length];
			for (int i = 0; i < codePaths.length; i++) {
				String codePath = codePaths[i].trim();
				log.debug("Searching task path '" + codePath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				Task task = ModelMgr.getTaskByCodePath(tx, codePath);
				// Enregistrement dans le tableau
				if (task == null)
					throw new ModelException(Strings.getString(
							"ModelMgr.errors.UNKNOWN_TASK", codePath)); //$NON-NLS-1$ //$NON-NLS-2$
				tasks[i] = task;
			}

			// Retour du résultat
			return tasks;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static TaskSums getTaskSums(Task task, Calendar fromDate,
			Calendar toDate) throws ModelException, DbException {
		log.info("getTaskSums(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Vérification de la tache (le chemin de la tache doit être le bon
			// pour
			// que le calcul le soit)
			if (task != null)
				checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Calcul des sommes
			TaskSums sums = DbMgr.getTaskSums(tx, task, fromDate, toDate);

			// Fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return sums;
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static String getTaskCodePath(Task task) throws ModelException,
			DbException {
		log.info("getTaskCodePath(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Le chemin de la tache et son numéro ne doivent pas avoir changés
			// pour pouvoir invoquer cette méthode (la modification des
			// attributs
			// n'est autorisée que pour les champs autres que le chemin et le
			// numéro.
			checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Construction du chemin
			String taskPath = getTaskCodePath(tx, task);

			// Commit et fin de la transaction
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return taskPath.toString();
		} finally {
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Construit le chemin de la tâche à partir des codes de tache.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param task
	 *            la tache dont on veut connaître le chemin.
	 * @return le chemin.
	 * @throws DbException
	 *             levé en cas d'incident technique avec la base de données.
	 */
	private static String getTaskCodePath(DbTransaction tx, Task task)
			throws DbException {
		// Construction
		StringBuffer taskPath = new StringBuffer(""); //$NON-NLS-1$
		Task cursor = task;
		while (cursor != null) {
			taskPath.insert(0, cursor.getCode());
			taskPath.insert(0, "/"); //$NON-NLS-1$
			cursor = DbMgr.getParentTask(tx, cursor);
		}

		// Retour du résultat
		return taskPath.toString();
	}

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
	public static void moveDownTask(Task task) throws ModelException,
			DbException {
		log.info("moveDownTask(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Le chemin de la tache et son numéro ne doivent pas avoir changés
			// pour pouvoir invoquer cette méthode (la modification des
			// attributs
			// n'est autorisée que pour les champs autres que le chemin et le
			// numéro.
			checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Recherche de la tache à descendre (incrémentation du numéro)
			byte taskToMoveUpNumber = (byte) (task.getNumber() + 1);
			Task taskToMoveUp = DbMgr.getTask(tx, task.getPath(),
					taskToMoveUpNumber);
			if (taskToMoveUp == null)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.TASK_CANNOT_BE_MOVED_DOWN")); //$NON-NLS-1$

			// Inversion des taches
			toggleTasks(tx, task, taskToMoveUp);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static void moveTaskUpOrDown(Task task, int newTaskNumber)
			throws ModelException, DbException {
		log.info("moveTaskUpOrDown(" + task + ", " + newTaskNumber + ")");
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Le chemin de la tache et son numéro ne doivent pas avoir changés
			// pour pouvoir invoquer cette méthode
			checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Pour que la méthode fonctionne, il faut que le nombre
			// cible soit différent du nombre courant
			if (task.getNumber() == newTaskNumber)
				throw new ModelException(
						"New task number is equal to current task number ; task not moved");

			// Récupération de la tache parent, et contrôle du modèle
			// (le numéro de destination ne peut être hors intervalle)
			Task parentTask = DbMgr.getParentTask(tx, task);
			if (newTaskNumber > parentTask.getSubTasksCount()
					|| newTaskNumber < 1)
				throw new ModelException("Invalid task number");

			// Définition du sens de déplacement
			int stepSign = task.getNumber() > newTaskNumber ? -1 : 1;
			for (int i = task.getNumber(); i != newTaskNumber + stepSign; i = i
					+ stepSign) {
				Task taskToToggle = DbMgr.getTask(tx, task.getPath(), (byte) i);
				toggleTasks(tx, task, taskToToggle);
				task.setNumber((byte) i);
			}

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}

	}

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
	public static synchronized void moveTask(Task task, Task destParentTask)
			throws ModelException, DbException {
		log.info("moveTask(" + task + ", " + destParentTask + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			/**
			 * Controles d'intégrité.
			 */

			// Le chemin de la tache et son numéro ne doivent pas avoir changés
			// pour pouvoir invoquer cette méthode (la modification des
			// attributs
			// n'est autorisée que pour les champs autres que le chemin et le
			// numéro.
			checkTaskPathAndUpdateSubTasksCount(tx, task);
			if (destParentTask != null)
				checkTaskPathAndUpdateSubTasksCount(tx, destParentTask);

			// Control : la tache de destination ne doit pas être
			// une tache fille de la tache à déplacer
			Task cursor = destParentTask;
			while (cursor != null) {
				if (cursor.equals(task))
					throw new ModelException(
							Strings.getString("ModelMgr.errors.TASK_CANNOT_BE_MOVED_UNDER_ITSELF")); //$NON-NLS-1$
				cursor = DbMgr.getParentTask(tx, cursor);
			}

			// Une tache ne peut admettre une sous-tache que si elle
			// n'est pas déja associée à un consommé
			if (destParentTask != null)
				checkAcceptsSubtasks(tx, destParentTask);

			// Le code de la tache à déplacer ne doit pas être en conflit
			// avec un code d'une autre tache fille de la tache parent
			// de destination
			String destPath = destParentTask != null ? destParentTask
					.getFullPath() : ""; //$NON-NLS-1$
			Task sameCodeTask = DbMgr.getTask(tx, destPath, task.getCode());
			if (sameCodeTask != null)
				throw new ModelException(
						Strings.getString(
								"ModelMgr.errors.TASK_CODE_EXIST_AT_DESTINATION", task.getCode())); //$NON-NLS-1$ //$NON-NLS-2$

			/**
			 * Déplacement de la tache.
			 */

			// Récupération de la tache parent et des sous-taches
			// avant modification de son numéro et de son chemin
			String initialTaskFullPath = task.getFullPath();
			Task srcParentTask = DbMgr.getParentTask(tx, task);
			Task[] subTasksToMove = DbMgr.getSubtasks(tx, task);

			// Déplacement de la tache
			byte number = DbMgr.newTaskNumber(tx, destPath);
			task.setPath(destPath);
			task.setNumber(number);
			DbMgr.updateTask(tx, task);

			// Déplacement des sous-taches
			changeTasksPaths(tx, subTasksToMove, initialTaskFullPath.length(),
					task.getFullPath());

			// Reconstruction des numéros de tâches d'où la tâche provenait
			// et qui a laissé un 'trou' en étant déplacée
			rebuildSubtasksNumbers(tx, srcParentTask);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static void moveUpTask(Task task) throws ModelException, DbException {
		log.info("moveUpTask(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Le chemin de la tache et son numéro ne doivent pas avoir changés
			// pour pouvoir invoquer cette méthode (la modification des
			// attributs
			// n'est autorisée que pour les champs autres que le chemin et le
			// numéro.
			checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Recherche de la tache à monter (décrémentation du numéro)
			byte taskToMoveDownNumber = (byte) (task.getNumber() - 1);
			Task taskToMoveDown = DbMgr.getTask(tx, task.getPath(),
					taskToMoveDownNumber);
			if (taskToMoveDown == null)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.TASK_CANNOT_BE_MOVED_UP")); //$NON-NLS-1$

			// Inversion des taches
			toggleTasks(tx, task, taskToMoveDown);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Reconstruit les numéros de taches pour un chemin donné (chemin complet de
	 * la tache parent considérée).
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param parentTask
	 *            la tache parent.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private static void rebuildSubtasksNumbers(DbTransaction tx, Task parentTask)
			throws DbException {
		// Récupération des sous-taches
		Task[] tasks = DbMgr.getSubtasks(tx, parentTask);
		for (int i = 0; i < tasks.length; i++) {
			Task task = tasks[i];
			byte taskNumber = task.getNumber();
			byte expectedNumber = (byte) (i + 1);
			if (taskNumber != expectedNumber) {
				Task[] subTasks = DbMgr.getSubtasks(tx, task);
				task.setNumber(expectedNumber);
				String fullPath = task.getFullPath();
				changeTasksPaths(tx, subTasks, fullPath.length(), fullPath);
				DbMgr.updateTask(tx, task);
			}
		}
	}

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
	public static void removeCollaborator(Collaborator collaborator)
			throws ModelException, DbException {
		log.info("removeCollaborator(" + collaborator + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Vérification que le collaborateur n'est pas utilisé
			long contribsNb = getContributionsNb(tx, null, collaborator, null,
					null, null);
			if (contribsNb != 0)
				throw new ModelException(
						Strings.getString(
								"ModelMgr.errros.COLLABORATOR_WITH_CONTRIBUTIONS_CANNOT_BE_REMOVED", new Long(contribsNb))); //$NON-NLS-1$ //$NON-NLS-2$

			// Suppression du collaborateur
			DbMgr.removeCollaborator(tx, collaborator);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static void removeContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DbException,
			ModelException {
		log.info("removeContribution(" + contribution + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Faut-il mettre à jour automatiquement le RAF de la tache ?
			if (!updateEstimatedTimeToComlete) {
				// Suppression de la contribution
				DbMgr.removeContribution(tx, contribution);
			} else {
				// Récupération des éléments de la contribution
				Collaborator contributor = DbMgr.getCollaborator(tx,
						contribution.getContributorId());
				Task task = DbMgr.getTask(tx, contribution.getTaskId());
				// Récupération de la contribution correspondante en base
				Contribution[] contributions = DbMgr.getContributions(tx,
						contributor, task, contribution.getDate(),
						contribution.getDate());
				if (contributions.length == 0) {
					// Si la contribution n'existait pas, il n'yu a rien à faire
					// de plus
				}
				// Sinon, il y a forcément une seule contribution
				else {
					// On vérifie que la donnée en base est en phase avec
					// l'entrant
					// pour s'assurer qu'on ne va pas incrémenter le RAF de la
					// tache
					// avec une valeur incohérente
					if (contribution.getDurationId() != contributions[0]
							.getDurationId())
						throw new ModelException(
								Strings.getString("ModelMgr.errors.CONTRIBUTION_UPDATE_DETECTED")); //$NON-NLS-1$

					// Suppression de la contribution
					DbMgr.removeContribution(tx, contribution);

					// Mise à jour du RAF de la tache
					task.setTodo(task.getTodo() + contribution.getDurationId());
					DbMgr.updateTask(tx, task);
				}
			}

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Supprime des contributions.
	 * 
	 * @param contributions
	 *            les contributions à supprimer.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static void removeContributions(Contribution[] contributions)
			throws DbException {
		log.info("removeContributions(" + contributions + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Suppression de la contribution
			for (int i = 0; i < contributions.length; i++)
				DbMgr.removeContribution(tx, contributions[i]);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static void removeDuration(Duration duration) throws ModelException,
			DbException {
		log.info("removeDuration(" + duration + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Suppression
			removeDuration(tx, duration);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Supprime une durée du référentiel de durées dans un contexte de
	 * transaction.
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param duration
	 *            la durée à supprimer.
	 * @throws ModelException
	 *             levé dans le cas ou la durée n'existe pas en base.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private static void removeDuration(DbTransaction tx, Duration duration)
			throws ModelException, DbException {
		// Vérification de l'existance
		if (!durationExists(tx, duration))
			throw new ModelException(
					Strings.getString("ModelMgr.errors.DURATION_DOES_NOT_EXIST")); //$NON-NLS-1$

		// Vérification de la non utilisation de la durée
		if (DbMgr.durationIsUsed(tx, duration))
			throw new ModelException(
					Strings.getString("ModelMgr.errors.UNMOVEABLE_DURATION")); //$NON-NLS-1$

		// Suppression
		DbMgr.removeDuration(tx, duration);
	}

	/**
	 * Supprime une tache.
	 * 
	 * <p>
	 * Cette méthode est synchronisé en raison de la modification potentielle
	 * du numéro de certaines taches.
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
	public static synchronized void removeTask(Task task) throws DbException,
			ModelException {
		log.info("removeTask(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Vérification de l'adéquation des attibuts de la tache avec les
			// données en base
			checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Vérification que la tache n'est pas utilisé
			long contribsNb = getContributionsNb(tx, task, null, null, null,
					null);
			if (contribsNb != 0)
				throw new ModelException(
						Strings.getString(
								"ModelMgr.errors.TASK_HAS_SUBTASKS", new Long(contribsNb))); //$NON-NLS-1$ //$NON-NLS-2$

			// Récupération de la tâche parent pour reconstruction des
			// numéros de taches
			Task parentTask = DbMgr.getParentTask(tx, task);

			// Suppression des taches et sous taches
			DbMgr.removeTask(tx, task);

			// Reconstruction des numéros de taches
			rebuildSubtasksNumbers(tx, parentTask);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Inverse deux taches dans l'arborescence des taches.
	 * 
	 * @param tx
	 *            contexte de transaction.
	 * @param task1
	 *            la 1° tache.
	 * @param task2
	 *            la 2nde tache.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	private static void toggleTasks(DbTransaction tx, Task task1, Task task2)
			throws DbException {
		byte task1InitialNumber = task1.getNumber();
		byte task2InitialNumber = task2.getNumber();
		String task1InitialFullpath = task1.getFullPath();
		String task2InitialFullpath = task2.getFullPath();

		// Récupération des taches filles de ces 2 taches
		Task[] task1subTasks = DbMgr.getSubtasks(tx, task1);
		Task[] task2subTasks = DbMgr.getSubtasks(tx, task2);

		// Changement des numéros de la tache 1 avec une valeur fictive
		task1.setNumber((byte) 0);
		DbMgr.updateTask(tx, task1);
		changeTasksPaths(tx, task1subTasks, task1InitialFullpath.length(),
				task1.getFullPath());

		// Changement des numéros de la tache 2
		task2.setNumber(task1InitialNumber);
		DbMgr.updateTask(tx, task2);
		changeTasksPaths(tx, task2subTasks, task2InitialFullpath.length(),
				task2.getFullPath());

		// Changement des numéros de la tache 1
		task1.setNumber(task2InitialNumber);
		DbMgr.updateTask(tx, task1);
		changeTasksPaths(tx, task1subTasks, task1InitialFullpath.length(),
				task1.getFullPath());
	}

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
	public static Collaborator updateCollaborator(Collaborator collaborator)
			throws DbException, ModelException {
		log.info("updateCollaborator(" + collaborator + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Control de l'unicité du login
			checkUniqueLogin(tx, collaborator);

			// Mise à jour des données
			collaborator = DbMgr.updateCollaborator(tx, collaborator);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return collaborator;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

	/**
	 * Met à jour une durée.
	 * 
	 * @param duration
	 *            la durée à mettre à jour.
	 * @return la durée mise à jour.
	 * @throws DbException
	 *             levé en cas d'incident technique d'accès à la base.
	 */
	public static Duration updateDuration(Duration duration) throws DbException {
		log.info("updateDuration(" + duration + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Mise à jour des données
			duration = DbMgr.updateDuration(tx, duration);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return duration;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static Contribution updateContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws DbException,
			ModelException {
		log.info("updateContribution(" + contribution + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// La durée existe-t-elle ?
			if (DbMgr.getDuration(tx, contribution.getDurationId()) == null) {
				throw new ModelException(
						Strings.getString("ModelMgr.errors.INVALID_DURATION")); //$NON-NLS-1$
			}
			
			Contribution result = null;
			// Faut-il mettre à jour automatiquement le RAF de la tache ?
			if (!updateEstimatedTimeToComlete) {
				// Mise à jour des données
				result = DbMgr.updateContribution(tx, contribution);
			} else {
				// Récupération des éléments de la contribution
				Collaborator contributor = DbMgr.getCollaborator(tx,
						contribution.getContributorId());
				Task task = DbMgr.getTask(tx, contribution.getTaskId());
				// Récupération de la contribution correspondante en base
				Contribution[] contributions = DbMgr.getContributions(tx,
						contributor, task, contribution.getDate(),
						contribution.getDate());
				if (contributions.length == 0) {
					// Si la contribution n'existe pas, c'est qu'il y a
					// déphasage entre les données de l'appelant et la BDD
					throw new ModelException(
							Strings.getString("ModelMgr.errors.CONTRIBUTION_DELETION_DETECTED")); //$NON-NLS-1$
				}
				// Sinon, il y a forcément une seule contribution
				else {
					long oldDuration = contributions[0].getDurationId();
					long newDuration = contribution.getDurationId();

					// Mise à jour de la contribution
					DbMgr.updateContribution(tx, contribution);

					// Mise à jour du RAF de la tache
					long newEtc = task.getTodo() + oldDuration - newDuration;
					task.setTodo(newEtc > 0 ? newEtc : 0);
					DbMgr.updateTask(tx, task);
				}
			}

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour du résultat
			return result;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static Contribution[] changeContributionTask(
			Contribution[] contributions, Task newContributionTask)
			throws DbException, ModelException {
		log.info("changeContributionTask(" + contributions + ", " + newContributionTask + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// La tache ne peut accepter une contribution que
			// si elle n'admet aucune sous-tache
			if (newContributionTask.getSubTasksCount() > 0)
				throw new ModelException(
						Strings.getString("ModelMgr.errors.A_TASK_WITH_SUBTASKS_CANNOT_ACCEPT_CONTRIBUTIONS")); //$NON-NLS-1$

			// Mise à jour des identifiants de tâche
			for (int i = 0; i < contributions.length; i++) {
				Contribution contribution = contributions[i];
				DbMgr.changeContributionTask(tx, contribution,
						newContributionTask);
			}

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour de la tache modifiée
			return contributions;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static Duration updateDuration(Duration duration,
			Duration newDuration) throws ModelException, DbException {
		log.info("updateDuration(" + duration + ", " + newDuration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DbTransaction tx = null;
		try {
			// Si la nouvelle durée est égale à l'ancienne, il n'y a rien
			// à faire de plus!...
			if (!newDuration.equals(duration)) {

				// Ouverture de la transaction
				tx = DbMgr.beginTransaction();

				// Tentative de suppression de la durée
				removeDuration(tx, duration);

				// Insertion de la nouvelle durée
				createDuration(tx, newDuration);

				// Commit et fin de la transaction
				DbMgr.commitTransaction(tx);
				DbMgr.endTransaction(tx);
				tx = null;
			}
			// Retour de la tache modifiée
			return newDuration;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

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
	public static Task updateTask(Task task) throws ModelException, DbException {
		log.info("updateTask(" + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		DbTransaction tx = null;
		try {
			// Ouverture de la transaction
			tx = DbMgr.beginTransaction();

			// Le chemin de la tache et son numéro ne doivent pas avoir changés
			// pour pouvoir invoquer cette méthode (la modification des
			// attributs
			// n'est autorisée que pour les champs autres que le chemin et le
			// numéro.
			checkTaskPathAndUpdateSubTasksCount(tx, task);

			// Check sur l'unicité du code pour le chemin considéré
			Task parentTask = DbMgr.getParentTask(tx, task);
			Task sameCodeTask = DbMgr
					.getTask(tx, parentTask != null ? parentTask.getFullPath()
							: "", task.getCode()); //$NON-NLS-1$
			if (sameCodeTask != null && !sameCodeTask.equals(task))
				throw new ModelException(
						Strings.getString("ModelMgr.errors.TASK_CODE_ALREADY_IN_USE")); //$NON-NLS-1$

			// Mise à jour des données
			task = DbMgr.updateTask(tx, task);

			// Commit et fin de la transaction
			DbMgr.commitTransaction(tx);
			DbMgr.endTransaction(tx);
			tx = null;

			// Retour de la tache modifiée
			return task;
		} finally {
			if (tx != null)
				try {
					DbMgr.rollbackTransaction(tx);
				} catch (DbException ignored) {
				}
			if (tx != null)
				try {
					DbMgr.endTransaction(tx);
				} catch (DbException ignored) {
				}
		}
	}

}
