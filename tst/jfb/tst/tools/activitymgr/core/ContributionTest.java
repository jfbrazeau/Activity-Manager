package jfb.tst.tools.activitymgr.core;

import java.util.Calendar;
import java.util.GregorianCalendar;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tst.tools.activitymgr.AbstractModelTestCase;

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
	private long[] durations;
	
	/** Contributions */
	private Contribution c1;
	private Contribution c2;
	private Contribution c3;
	
	private void createSampleObjects(boolean createContributions) throws DbException, ModelException {
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
		task111.setTodo(25);
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
		durations = ModelMgr.getDurations();
		long duration = durations[0];
		
		// Création de contributions
		if (createContributions) {
			Calendar date = new GregorianCalendar();
			
			c1 = new Contribution();
			c1.setDate(date);
			c1.setContributorId(col1.getId());
			c1.setDuration(duration);
			c1.setTaskId(task111.getId());
			ModelMgr.createContribution(c1);

			date.add(Calendar.DATE, 1);
			c2 = new Contribution();
			c2.setDate(date);
			c2.setContributorId(col2.getId());
			c2.setDuration(duration);
			c2.setTaskId(task112.getId());
			ModelMgr.createContribution(c2);
		
			date.add(Calendar.MONTH, 1);
			c3 = new Contribution();
			c3.setDate(date);
			c3.setContributorId(col2.getId());
			c3.setDuration(duration);
			c3.setTaskId(task111.getId());
			ModelMgr.createContribution(c3);
		}

	}
	
	public void removeSampleObjects() throws DbException, ModelException {
		if (c1!=null)
			ModelMgr.removeContribution(c1);
		if (c2!=null)
			ModelMgr.removeContribution(c2);
		if (c2!=null)
			ModelMgr.removeContribution(c3);
		removeRecursively(rootTask);
		ModelMgr.removeCollaborator(col1);
		ModelMgr.removeCollaborator(col2);
	}
	
	private static void removeRecursively(Task task) throws DbException, ModelException {
		// Récupération des taches filles
		Task[] subTasks = ModelMgr.getSubtasks(task);
		for (int i=subTasks.length-1; i>=0; i--) {
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
		Calendar cal = new GregorianCalendar(year, month-1, day);
		
		// Test...
		Contribution c = new Contribution();
		c.setContributorId(col1.getId());
		c.setDuration(durations[0]);
		c.setDate(cal);

		// Vérification du calendrier
		assertEquals(year, c.getYear());
		assertEquals(month, c.getMonth());
		assertEquals(day, c.getDay());
		
		// Création de la contribution ur une tache avec de sous taches
		try {
			c.setTaskId(rootTask.getId());
			c = ModelMgr.createContribution(c);
			fail("A tasks that admit sub tasks must not accept a contribution");
		}
		catch (ModelException expected) {}
		
		// Création de la contribution sur une tache sans sous taches
		c.setTaskId(task111.getId());
		c = ModelMgr.createContribution(c);

		// Recherche de cette contribution
		Contribution[] cs = ModelMgr.getDaysContributions(col1, task111, cal, cal);
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(cs[0], c);

		// Suppression
		ModelMgr.removeContribution(c);
		
		// Nouvelle recherche => à présent, la recherche ne doit rien ramener
		cs = ModelMgr.getDaysContributions(col1, task111, cal, cal);
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertNull(cs[0]);

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
		cs = ModelMgr.getContributions(task111, col1, new Integer(c1.getYear()), new Integer(c1.getMonth()), new Integer(c1.getDay()));
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Test requête avec le jour et la tache racine
		cs = ModelMgr.getContributions(rootTask, null, new Integer(c1.getYear()), new Integer(c1.getMonth()), new Integer(c1.getDay()));
		assertNotNull(cs);
		assertEquals(1, cs.length);
		assertEquals(c1, cs[0]);

		// Suppression des taches de test
		removeSampleObjects();
	}

	public void testChangeContributionsTask() throws DbException, ModelException {
		// Création des taches de test
		createSampleObjects(true);
		
		// Vérification avant mise à jour
		assertEquals(c1.getTaskId(), task111.getId());
		assertEquals(c2.getTaskId(), task112.getId());
		assertEquals(c3.getTaskId(), task111.getId());

		// Changement des contributions
		Contribution[] cs = new Contribution[] {
				c1, c2, c3
		};
		ModelMgr.changeContributionTask(cs, task112);	
		
		// Vérification après mise à jour
		assertEquals(c1.getTaskId(), task112.getId());
		assertEquals(c2.getTaskId(), task112.getId());
		assertEquals(c3.getTaskId(), task112.getId());

		// Suppression des taches de test
		removeSampleObjects();
	}
}
