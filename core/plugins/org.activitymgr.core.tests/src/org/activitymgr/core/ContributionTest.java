package org.activitymgr.core;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.IntervalContributions;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.model.ModelException;

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
			throws ModelException {
		// Création des tâches de test
		// ROOT
		// + T1
		// | + T11
		// | + T111
		// | + T112
		// + T2
		rootTask = getModelMgr().createNewTask(null);
		rootTask.setName("Root task");
		rootTask = getModelMgr().updateTask(rootTask);

		task1 = getFactory().newTask();
		task1.setCode("T1");
		task1.setName("Task 1");
		task1 = getModelMgr().createTask(rootTask, task1);

		task11 = getFactory().newTask();
		task11.setCode("T11");
		task11.setName("Task 11");
		task11 = getModelMgr().createTask(task1, task11);

		task111 = getFactory().newTask();
		task111.setCode("T111");
		task111.setName("Task 111");
		task111.setBudget(30);
		task111.setInitiallyConsumed(5);
		task111.setTodo(1000);
		task111 = getModelMgr().createTask(task11, task111);

		task112 = getFactory().newTask();
		task112.setCode("T112");
		task112.setName("Task 112");
		task112.setBudget(30);
		task112.setInitiallyConsumed(5);
		task112.setTodo(25);
		task112 = getModelMgr().createTask(task11, task112);

		task2 = getFactory().newTask();
		task2.setCode("T2");
		task2.setName("Task 2");
		task2.setBudget(60);
		task2.setInitiallyConsumed(10);
		task2.setTodo(50);
		task2 = getModelMgr().createTask(rootTask, task2);

		// Rechargement des taches pour mise à jour
		// des nombres de sous-taches
		rootTask = getModelMgr().getTask(rootTask.getId());
		task1 = getModelMgr().getTask(task1.getId());
		task11 = getModelMgr().getTask(task11.getId());
		task111 = getModelMgr().getTask(task111.getId());
		task112 = getModelMgr().getTask(task112.getId());
		task2 = getModelMgr().getTask(task2.getId());

		// Création de 2 collaborateurs
		col1 = getModelMgr().createNewCollaborator();
		col1.setFirstName("ColFN1");
		col1.setLastName("ColLN1");
		col1 = getModelMgr().updateCollaborator(col1);

		col2 = getModelMgr().createNewCollaborator();
		col2.setFirstName("ColFN2");
		col2.setLastName("ColLN2");
		col2 = getModelMgr().updateCollaborator(col2);

		// Récupération des durées
		duration1 = getFactory().newDuration();
		duration1.setId(100);
		duration1 = getModelMgr().createDuration(duration1);
		duration2 = getFactory().newDuration();
		duration2.setId(50);
		duration2 = getModelMgr().createDuration(duration2);

		// Création de contributions
		if (createContributions) {
			Calendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

			c1 = getFactory().newContribution();
			c1.setDate(date);
			c1.setContributorId(col1.getId());
			c1.setDurationId(duration1.getId());
			c1.setTaskId(task111.getId());
			getModelMgr().createContribution(c1, false);

			date.add(Calendar.DATE, 1);
			c2 = getFactory().newContribution();
			c2.setDate(date);
			c2.setContributorId(col2.getId());
			c2.setDurationId(duration1.getId());
			c2.setTaskId(task112.getId());
			getModelMgr().createContribution(c2, false);

			date.add(Calendar.MONTH, 1);
			c3 = getFactory().newContribution();
			c3.setDate(date);
			c3.setContributorId(col2.getId());
			c3.setDurationId(duration1.getId());
			c3.setTaskId(task111.getId());
			getModelMgr().createContribution(c3, false);
		}

	}

	protected void removeSampleObjects() throws ModelException {
		if (c1 != null)
			getModelMgr().removeContribution(c1, false);
		if (c2 != null)
			getModelMgr().removeContribution(c2, false);
		if (c3 != null)
			getModelMgr().removeContribution(c3, false);
		removeRecursively(rootTask);
		getModelMgr().removeCollaborator(col1);
		getModelMgr().removeCollaborator(col2);
		getModelMgr().removeDuration(duration1);
		getModelMgr().removeDuration(duration2);
	}

	private void removeRecursively(Task task) throws ModelException {
		// Suppression de la tache
		getModelMgr().removeTask(task);
	}

	public void testCreate() throws ModelException {
		// Création des taches de test
		createSampleObjects(false);

		int year = 2005;
		int month = 6;
		int day = 13;
		Calendar cal = new GregorianCalendar(year, month - 1, day);

		// Test...
		Contribution c = getFactory().newContribution();
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
			c = getModelMgr().createContribution(c, false);
			fail("A tasks that admits sub tasks must not accept a contribution");
		} catch (ModelException expected) {
		}

		// Création de la contribution sur une tache sans sous taches
		c.setTaskId(task111.getId());
		c = getModelMgr().createContribution(c, true);

		// Recherche de cette contribution
		IntervalContributions ic = getModelMgr().getIntervalContributions(col1,
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
		task111 = getModelMgr().getTask(task111.getId());
		assertEquals(oldEtc - c.getDurationId(), task111.getTodo());

		// Suppression
		getModelMgr().removeContribution(c, true);

		// Vérification de la mise à jour du RAF de la tache en base
		task111 = getModelMgr().getTask(task111.getId());
		assertEquals(oldEtc, task111.getTodo());

		// Nouvelle recherche => à présent, la recherche ne doit rien ramener
		ic = getModelMgr().getIntervalContributions(col1, task111, cal, cal);
		assertNotNull(ic);
		tcs = ic.getTaskContributions();
		assertNotNull(tcs);
		assertEquals(0, tcs.length);

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testRemove() throws ModelException {
		// Création des taches de test
		createSampleObjects(false);

		// Création d'une contribution
		Calendar date = new GregorianCalendar();
		Contribution c1 = getFactory().newContribution();
		c1.setDate(date);
		c1.setContributorId(col1.getId());
		c1.setDurationId(100);
		c1.setTaskId(task111.getId());
		getModelMgr().createContribution(c1, false);

		// Suppression avec une contribution non en phase
		// avec celle en BDD (une exception doit être levée)
		try {
			c1.setDurationId(25);
			getModelMgr().removeContribution(c1, true);
			fail("L'écart entre la durée de la contribution par rapport aux données en base aurait du provoquer la levée d'une erreur");
		} catch (ModelException e) {
			// On ne fait rien, l'exception doit être levée (on remet
			// tout de même la durée de la contribution à sa valeur initiale)
			c1.setDurationId(100);
		}

		// Supression sans MAJ du RAF de la tache
		getModelMgr().removeContribution(c1, false);
		long currentTodo = task111.getTodo();
		task111 = getModelMgr().getTask(task111.getId());
		assertEquals(currentTodo, task111.getTodo());

		// Recréation de la contribution
		getModelMgr().createContribution(c1, false);

		// Supression avec MAJ du RAF de la tache
		getModelMgr().removeContribution(c1, true);
		currentTodo = task111.getTodo();
		task111 = getModelMgr().getTask(task111.getId());
		assertEquals(currentTodo + c1.getDurationId(), task111.getTodo());

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testUpdate() throws ModelException {
		// Création des taches de test
		createSampleObjects(true);

		// Récupération du RAF de la tache
		task111 = getModelMgr().getTask(task111.getId());
		long initialEtc = task111.getTodo();

		// Mise à jour de la contribution sans changement du RAF
		c1.setDurationId(50);
		getModelMgr().updateContribution(c1, false);

		// Vérification que le RAF de la tache n'a pas changé
		task111 = getModelMgr().getTask(task111.getId());
		assertEquals(initialEtc, task111.getTodo());

		// Vérification de la mise à jour en base
		IntervalContributions ic = getModelMgr().getIntervalContributions(col1,
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
		getModelMgr().updateContribution(c1, true);

		// Vérification que le RAF de la tache a bien changé
		// la différence doit être égale à la différence
		task111 = getModelMgr().getTask(task111.getId());
		assertEquals(100 - 50, initialEtc - task111.getTodo());

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testGetContributions() throws ModelException {
		// Création des taches de test
		createSampleObjects(true);

		Contribution[] cs = null;

		// Test requête avec tache racine
		cs = getModelMgr().getContributions(null, rootTask, null, null);
		assertNotNull(cs);
		assertEquals(3, cs.length);
		assertEquals(c1, cs[0]);
		assertEquals(c2, cs[1]);
		assertEquals(c3, cs[2]);

		// Test requête avec une tache
		cs = getModelMgr().getContributions(null, task111, null, null);
		assertNotNull(cs);
		assertEquals(2, cs.length);
		assertEquals(c1, cs[0]);
		assertEquals(c3, cs[1]);

		// Test requête avec une tache et un collaborateur
		cs = getModelMgr().getContributions(col1, task111, null, null);
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Test requête avec une tache et un collaborateur et un mois
		Calendar cal = new GregorianCalendar(c1.getYear(), c1.getMonth() - 1,
				c1.getDay());
		cs = getModelMgr().getContributions(col1, task111, cal, cal);
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Test requête avec le jour et la tache racine
		cs = getModelMgr().getContributions(null, rootTask,
				cal, cal);
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Test requête avec le jour et la tache racine
		cs = getModelMgr().getContributions(null, rootTask,
				null, cal);
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Test requête avec le jour et la tache racine
		cs = getModelMgr().getContributions(null, rootTask,
				cal, null);
		assertNotNull(cs);
		assertEquals(3, cs.length); // 3 contributions
		assertEquals(c1, cs[0]);

		// Test requête avec le jour et la tache racine
		cs = getModelMgr().getContributions(null, rootTask,
				null, null);
		assertNotNull(cs);
		assertEquals(3, cs.length); // 3 contributions
		assertEquals(c1, cs[0]);

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testChangeContributionsTask() throws ModelException {
		// Création des taches de test
		createSampleObjects(true);

		// Vérification avant mise à jour
		assertEquals(c1.getTaskId(), task111.getId());
		assertEquals(c2.getTaskId(), task112.getId());
		assertEquals(c3.getTaskId(), task111.getId());

		// Changement des contributions
		Contribution[] cs = new Contribution[] { c1, c2, c3 };
		getModelMgr().changeContributionTask(cs, task112);

		// Vérification après mise à jour
		assertEquals(c1.getTaskId(), task112.getId());
		assertEquals(c2.getTaskId(), task112.getId());
		assertEquals(c3.getTaskId(), task112.getId());

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testCountDaysWhenYearHasMoreThan365Days() throws ModelException {
		// Création des taches de test
		createSampleObjects(true);

		// A contribution on 1st January 2013
		Calendar date = new GregorianCalendar(2013, 0, 1);
		Contribution c = getFactory().newContribution();
		c.setTaskId(task111.getId());
		c.setContributorId(col1.getId());
		c.setDurationId(100);
		c.setDate(date);
		c = getModelMgr().createContribution(c, false);

		// Retrieve interval contributions
		IntervalContributions ic = getModelMgr().getIntervalContributions(col1,
				null, new GregorianCalendar(2012, 11, 24), // 24th December 2012
				new GregorianCalendar(2013, 0, 6)); // 6 January 2013
		assertNotNull(ic);
		assertNotNull(ic.getTaskContributions());
		assertEquals(1, ic.getTaskContributions().length);
		assertNotNull(ic.getTaskContributions()[0]);
		assertNotNull(ic.getTaskContributions()[0].getContributions());
		// Expected size : 14 days
		assertEquals(14, ic.getTaskContributions()[0].getContributions().length);

		// Suppression des taches de test
		getModelMgr().removeContribution(c, false);
		removeSampleObjects();
	}
	
	public void testCountDaysWhenHourChanges() throws ModelException {
		// Création des taches de test
		createSampleObjects(true);

		// A contribution on 27th March 2012
		Calendar date = new GregorianCalendar(2012, 2, 27);
		Contribution c = getFactory().newContribution();
		c.setTaskId(task111.getId());
		c.setContributorId(col1.getId());
		c.setDurationId(100);
		c.setDate(date);
		c = getModelMgr().createContribution(c, false);

		// Retrieve interval contributions
		IntervalContributions ic = getModelMgr().getIntervalContributions(col1,
				null, new GregorianCalendar(2012, 2, 19), // 19th March 2012
				new GregorianCalendar(2012, 3, 1)); // 1st Arpril 2012
		assertNotNull(ic);
		assertNotNull(ic.getTaskContributions());
		assertEquals(1, ic.getTaskContributions().length);
		assertNotNull(ic.getTaskContributions()[0]);
		assertNotNull(ic.getTaskContributions()[0].getContributions());
		// Expected size : 14 days
		assertEquals(14, ic.getTaskContributions()[0].getContributions().length);

		// Suppression des taches de test
		getModelMgr().removeContribution(c, false);
		removeSampleObjects();
	}

	public void testGetContributedTasks() throws ModelException {
		Calendar today = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		// Création des taches de test
		createSampleObjects(true);

		// c1 => col1, task111, today
		// c2 => col2, task112, today+1day
		// c3 => col2, task111, today+1month
		
		// All parameters null
		Task[] tasks = getModelMgr().getContributedTasks(null, null, null);
		assertNotNull(tasks);
		assertEquals(2, tasks.length);

		// Collaborator given
		tasks = getModelMgr().getContributedTasks(col1, null, null);
		assertNotNull(tasks);
		assertEquals(1, tasks.length);
		assertEquals(task111.getId(), tasks[0].getId());

		// Interval given
		tasks = getModelMgr().getContributedTasks(null, today, today);
		assertNotNull(tasks);
		assertEquals(1, tasks.length);
		assertEquals(task111.getId(), tasks[0].getId());

		// All parameters given
		tasks = getModelMgr().getContributedTasks(col1, today, today);
		assertNotNull(tasks);
		assertEquals(1, tasks.length);
		assertEquals(task111.getId(), tasks[0].getId());

		// All parameters given, empty result
		tasks = getModelMgr().getContributedTasks(col2, today, today);
		assertNotNull(tasks);
		assertEquals(0, tasks.length);

		// Missing end date
		tasks = getModelMgr().getContributedTasks(col2, today, null);
		assertNotNull(tasks);
		assertEquals(2, tasks.length);

		// Missing start date and collaborator
		tasks = getModelMgr().getContributedTasks(null, null, today);
		assertNotNull(tasks);
		assertEquals(1, tasks.length);
		assertEquals(task111.getId(), tasks[0].getId());

		// Remove sample objects
		removeSampleObjects();
	}

	public void testGetContributors() throws ModelException {
		Calendar today = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		// Création des taches de test
		createSampleObjects(true);

		// c1 => col1, task111, today
		// c2 => col2, task112, today+1day
		// c3 => col2, task111, today+1month

		// All parameters null
		Collaborator[] contributors = getModelMgr().getContributors(null, null, null);
		assertNotNull(contributors);
		assertEquals(2, contributors.length);

		// Collaborator given
		contributors = getModelMgr().getContributors(task111, null, null);
		assertNotNull(contributors);
		assertEquals(2, contributors.length);

		// Interval given
		contributors = getModelMgr().getContributors(null, today, today);
		assertNotNull(contributors);
		assertEquals(1, contributors.length);
		assertEquals(col1.getId(), contributors[0].getId());

		// All parameters given
		contributors = getModelMgr().getContributors(task111, today, today);
		assertNotNull(contributors);
		assertEquals(1, contributors.length);
		assertEquals(col1.getId(), contributors[0].getId());

		// All parameters given, empty result
		contributors = getModelMgr().getContributors(task112, today, today);
		assertNotNull(contributors);
		assertEquals(0, contributors.length);

		// Missing end date
		contributors = getModelMgr().getContributors(task111, today, null);
		assertNotNull(contributors);
		assertEquals(2, contributors.length);

		// Missing start date and collaborator
		contributors = getModelMgr().getContributors(null, null, today);
		assertNotNull(contributors);
		assertEquals(1, contributors.length);
		assertEquals(col1.getId(), contributors[0].getId());

		// Remove sample objects
		removeSampleObjects();
	}
	
	public void testGetContributionYears() throws ModelException {
		Calendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		// Création des taches de test
		createSampleObjects(false);
		
		// Empty contributions
		Collection<Integer> contributionYears = getModelMgr().getContributionYears();
		assertNotNull(contributionYears);
		assertEquals(0, contributionYears.size());

		// Create a contribution in 2010
		c1 = getFactory().newContribution();
		date.set(Calendar.YEAR, 2010);
		c1.setDate(date);
		c1.setContributorId(col1.getId());
		c1.setDurationId(duration1.getId());
		c1.setTaskId(task111.getId());
		getModelMgr().createContribution(c1, false);

		contributionYears = getModelMgr().getContributionYears();
		assertNotNull(contributionYears);
		assertEquals(1, contributionYears.size());
		Iterator<Integer> iterator = contributionYears.iterator();
		assertEquals(2010, (int)iterator.next());

		// Create a contribution in 2020
		c2 = getFactory().newContribution();
		date.set(Calendar.YEAR, 2020);
		c2.setDate(date);
		c2.setContributorId(col1.getId());
		c2.setDurationId(duration1.getId());
		c2.setTaskId(task111.getId());
		getModelMgr().createContribution(c2, false);

		contributionYears = getModelMgr().getContributionYears();
		assertNotNull(contributionYears);
		assertEquals(2, contributionYears.size());
		iterator = contributionYears.iterator();
		assertEquals(2010, (int)iterator.next());
		assertEquals(2020, (int)iterator.next());
		
		// Remove sample objects
		removeSampleObjects();
	}
}
