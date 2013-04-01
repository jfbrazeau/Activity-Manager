package org.activitymgr.core;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;

public class ContributionTest extends AbstractModelTestCase {

	/** Taches de test */
	private Task rootTask;
	private Task task1;
	private Task task11;
	private Task task111;
	private Task task112;
	private Task task2;

	/** Collaborateurs de test */
	private Collaborator col1;
	private Collaborator col2;

	/** Durées de test */
	private Duration duration1;
	private Duration duration2;

	/** Contributions */
	private Contribution c1;
	private Contribution c2;
	private Contribution c3;

	private void createSampleObjects(boolean createContributions)
			throws DbException, ModelException {
		// Création des tâches de test
		rootTask = ModelMgr.createNewTask(null);
		rootTask.setName("Root task");
		rootTask = ModelMgr.updateTask(rootTask);

		task1 = new Task();
		task1.setCode("T1");
		task1.setName("Task 1");
		task1 = ModelMgr.createTask(rootTask, task1);

		task11 = new Task();
		task11.setCode("T11");
		task11.setName("Task 11");
		task11 = ModelMgr.createTask(task1, task11);

		task111 = new Task();
		task111.setCode("T111");
		task111.setName("Task 111");
		task111.setBudget(30);
		task111.setInitiallyConsumed(5);
		task111.setTodo(1000);
		task111 = ModelMgr.createTask(task11, task111);

		task112 = new Task();
		task112.setCode("T112");
		task112.setName("Task 112");
		task112.setBudget(30);
		task112.setInitiallyConsumed(5);
		task112.setTodo(25);
		task112 = ModelMgr.createTask(task11, task112);

		task2 = new Task();
		task2.setCode("T2");
		task2.setName("Task 2");
		task2.setBudget(60);
		task2.setInitiallyConsumed(10);
		task2.setTodo(50);
		task2 = ModelMgr.createTask(rootTask, task2);

		// Rechargement des taches pour mise à jour
		// des nombres de sous-taches
		rootTask = ModelMgr.getTask(rootTask.getId());
		task1 = ModelMgr.getTask(task1.getId());
		task11 = ModelMgr.getTask(task11.getId());
		task111 = ModelMgr.getTask(task111.getId());
		task112 = ModelMgr.getTask(task112.getId());
		task2 = ModelMgr.getTask(task2.getId());

		// Création de 2 collaborateurs
		col1 = ModelMgr.createNewCollaborator();
		col1.setFirstName("ColFN1");
		col1.setLastName("ColLN1");
		col1 = ModelMgr.updateCollaborator(col1);

		col2 = ModelMgr.createNewCollaborator();
		col2.setFirstName("ColFN2");
		col2.setLastName("ColLN2");
		col2 = ModelMgr.updateCollaborator(col2);

		// Récupération des durées
		duration1 = new Duration();
		duration1.setId(100);
		duration1 = ModelMgr.createDuration(duration1);
		duration2 = new Duration();
		duration2.setId(50);
		duration2 = ModelMgr.createDuration(duration2);

		// Création de contributions
		if (createContributions) {
			Calendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

			c1 = new Contribution();
			c1.setDate(date);
			c1.setContributorId(col1.getId());
			c1.setDurationId(duration1.getId());
			c1.setTaskId(task111.getId());
			ModelMgr.createContribution(c1, false);

			date.add(Calendar.DATE, 1);
			c2 = new Contribution();
			c2.setDate(date);
			c2.setContributorId(col2.getId());
			c2.setDurationId(duration1.getId());
			c2.setTaskId(task112.getId());
			ModelMgr.createContribution(c2, false);

			date.add(Calendar.MONTH, 1);
			c3 = new Contribution();
			c3.setDate(date);
			c3.setContributorId(col2.getId());
			c3.setDurationId(duration1.getId());
			c3.setTaskId(task111.getId());
			ModelMgr.createContribution(c3, false);
		}

	}

	protected void removeSampleObjects() throws DbException, ModelException {
		if (c1 != null)
			ModelMgr.removeContribution(c1, false);
		if (c2 != null)
			ModelMgr.removeContribution(c2, false);
		if (c2 != null)
			ModelMgr.removeContribution(c3, false);
		removeRecursively(rootTask);
		ModelMgr.removeCollaborator(col1);
		ModelMgr.removeCollaborator(col2);
		ModelMgr.removeDuration(duration1);
		ModelMgr.removeDuration(duration2);
	}

	private static void removeRecursively(Task task) throws DbException,
			ModelException {
		// Récupération des taches filles
		Task[] subTasks = ModelMgr.getSubtasks(task);
		for (int i = subTasks.length - 1; i >= 0; i--) {
			Task subTask = subTasks[i];
			// Suppression des taches filles
			removeRecursively(subTask);
		}
		// Suppression de la tache
		ModelMgr.removeTask(task);
	}

	public void testCreate() throws DbException, ModelException {
		// Création des taches de test
		createSampleObjects(false);

		int year = 2005;
		int month = 6;
		int day = 13;
		Calendar cal = new GregorianCalendar(year, month - 1, day);

		// Test...
		Contribution c = new Contribution();
		c.setContributorId(col1.getId());
		c.setDurationId(duration1.getId());
		c.setDate(cal);

		// Vérification du calendrier
		assertEquals(year, c.getYear());
		assertEquals(month, c.getMonth());
		assertEquals(day, c.getDay());

		// Création de la contribution ur une tache avec de sous taches
		try {
			c.setTaskId(rootTask.getId());
			c = ModelMgr.createContribution(c, false);
			fail("A tasks that admits sub tasks must not accept a contribution");
		} catch (ModelException expected) {
		}

		// Création de la contribution sur une tache sans sous taches
		c.setTaskId(task111.getId());
		c = ModelMgr.createContribution(c, true);

		// Recherche de cette contribution
		IntervalContributions ic = ModelMgr.getIntervalContributions(col1,
				task111, cal, cal);
		assertNotNull(ic);
		TaskContributions[] tcs = ic.getTaskContributions();
		assertNotNull(tcs);
		assertEquals(1, tcs.length);
		TaskContributions tc = tcs[0];
		assertNotNull(tc);
		Contribution[] cs = tc.getContributions();
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(cs[0], c);

		// Vérification de la mise à jour du RAF de la tache en base
		long oldEtc = task111.getTodo();
		task111 = ModelMgr.getTask(task111.getId());
		assertEquals(oldEtc - c.getDurationId(), task111.getTodo());

		// Suppression
		ModelMgr.removeContribution(c, true);

		// Vérification de la mise à jour du RAF de la tache en base
		task111 = ModelMgr.getTask(task111.getId());
		assertEquals(oldEtc, task111.getTodo());

		// Nouvelle recherche => à présent, la recherche ne doit rien ramener
		ic = ModelMgr.getIntervalContributions(col1,
				task111, cal, cal);
		assertNotNull(ic);
		tcs = ic.getTaskContributions();
		assertNotNull(tcs);
		assertEquals(0, tcs.length);

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testRemove() throws DbException, ModelException {
		// Création des taches de test
		createSampleObjects(false);

		// Création d'une contribution
		Calendar date = new GregorianCalendar();
		Contribution c1 = new Contribution();
		c1.setDate(date);
		c1.setContributorId(col1.getId());
		c1.setDurationId(100);
		c1.setTaskId(task111.getId());
		ModelMgr.createContribution(c1, false);

		// Suppression avec une contribution non en phase
		// avec celle en BDD (une exception doit être levée)
		try {
			c1.setDurationId(25);
			ModelMgr.removeContribution(c1, true);
			fail("L'écart entre la durée de la contribution par rapport aux données en base aurait du provoquer la levée d'une erreur");
		} catch (ModelException e) {
			// On ne fait rien, l'exception doit être levée (on remet
			// tout de même la durée de la contribution à sa valeur initiale)
			c1.setDurationId(100);
		}

		// Supression sans MAJ du RAF de la tache
		ModelMgr.removeContribution(c1, false);
		long currentTodo = task111.getTodo();
		task111 = ModelMgr.getTask(task111.getId());
		assertEquals(currentTodo, task111.getTodo());

		// Recréation de la contribution
		ModelMgr.createContribution(c1, false);

		// Supression avec MAJ du RAF de la tache
		ModelMgr.removeContribution(c1, true);
		currentTodo = task111.getTodo();
		task111 = ModelMgr.getTask(task111.getId());
		assertEquals(currentTodo + c1.getDurationId(), task111.getTodo());

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testUpdate() throws DbException, ModelException {
		// Création des taches de test
		createSampleObjects(true);

		// Récupération du RAF de la tache
		task111 = ModelMgr.getTask(task111.getId());
		long initialEtc = task111.getTodo();

		// Mise à jour de la contribution sans changement du RAF
		c1.setDurationId(50);
		ModelMgr.updateContribution(c1, false);

		// Vérification que le RAF de la tache n'a pas changé
		task111 = ModelMgr.getTask(task111.getId());
		assertEquals(initialEtc, task111.getTodo());

		// Vérification de la mise à jour en base
		IntervalContributions ic = ModelMgr.getIntervalContributions(col1,
				task111, c1.getDate(), c1.getDate());
		assertNotNull(ic);
		TaskContributions[] tcs = ic.getTaskContributions();
		assertNotNull(tcs);
		assertEquals(1, tcs.length);
		TaskContributions tc = tcs[0];
		assertNotNull(tc);
		Contribution[] cs = tc.getContributions();
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(50, cs[0].getDurationId());

		// Nouvelle mise à jour de la contribution avec changement du RAF
		c1.setDurationId(100);
		ModelMgr.updateContribution(c1, true);

		// Vérification que le RAF de la tache a bien changé
		// la différence doit être égale à la différence
		task111 = ModelMgr.getTask(task111.getId());
		assertEquals(100 - 50, initialEtc - task111.getTodo());

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testGetContributions() throws DbException, ModelException {
		// Création des taches de test
		createSampleObjects(true);

		Contribution[] cs = null;

		// Test requête avec tache racine
		cs = ModelMgr.getContributions(rootTask, null, null, null, null);
		assertNotNull(cs);
		assertEquals(3, cs.length);
		assertEquals(c1, cs[0]);
		assertEquals(c2, cs[1]);
		assertEquals(c3, cs[2]);

		// Test requête avec une tache
		cs = ModelMgr.getContributions(task111, null, null, null, null);
		assertNotNull(cs);
		assertEquals(2, cs.length);
		assertEquals(c1, cs[0]);
		assertEquals(c3, cs[1]);

		// Test requête avec une tache et un collaborateur
		cs = ModelMgr.getContributions(task111, col1, null, null, null);
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Test requête avec une tache et un collaborateur et un mois
		cs = ModelMgr.getContributions(task111, col1,
				new Integer(c1.getYear()), new Integer(c1.getMonth()),
				new Integer(c1.getDay()));
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Test requête avec le jour et la tache racine
		cs = ModelMgr.getContributions(rootTask, null,
				new Integer(c1.getYear()), new Integer(c1.getMonth()),
				new Integer(c1.getDay()));
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testChangeContributionsTask() throws DbException,
			ModelException {
		// Création des taches de test
		createSampleObjects(true);

		// Vérification avant mise à jour
		assertEquals(c1.getTaskId(), task111.getId());
		assertEquals(c2.getTaskId(), task112.getId());
		assertEquals(c3.getTaskId(), task111.getId());

		// Changement des contributions
		Contribution[] cs = new Contribution[] { c1, c2, c3 };
		ModelMgr.changeContributionTask(cs, task112);

		// Vérification après mise à jour
		assertEquals(c1.getTaskId(), task112.getId());
		assertEquals(c2.getTaskId(), task112.getId());
		assertEquals(c3.getTaskId(), task112.getId());

		// Suppression des taches de test
		removeSampleObjects();
	}
}