package org.activitymgr.core;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskSearchFilter;
import org.activitymgr.core.beans.TaskSums;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.util.StringHelper;

public class TaskTest extends AbstractModelTestCase {

	/** Taches de test */
	private Task rootTask;
	private Task task1;
	private Task task11;
	private Task task111;
	private Task task112;
	private Task task2;

	public void testCreateRootTask() throws DAOException, ModelException {
		// Création des tâches de test
		Task task = getFactory().newTask();
		// Génération d'un code inutilisé
		task.setCode(String.valueOf(System.currentTimeMillis()).substring(8));
		task.setName("Root task");
		task = getModelMgr().createTask(null, task);
		
		// Vérification de la génération du numéro
		assertTrue(task.getNumber()>0);
		
		// Relecture de la tâche en base et comparaison
		long taskId = task.getId();
		Task otherTask = getModelMgr().getTask(taskId);
		assertEquals(task, otherTask); // Seuls les identifiants sont comparés
		assertEquals(task.getCode(), otherTask.getCode());
		assertEquals(task.getName(), otherTask.getName());
		assertEquals(task.getNumber(), otherTask.getNumber());
		
		// Le nombre de tache racine doit etre égal à 1
		assertEquals(1, getModelMgr().getRootTasksCount());
		
		// Suppression
		getModelMgr().removeTask(task);
		
		// Tentative de relecture (l'objet ne doit plus exister)
		assertNull(getModelMgr().getTask(taskId));
	}
	
	private void createSampleTasks() throws DAOException, ModelException {
		// Création des tâches de test
		rootTask = getModelMgr().createNewTask(null);
		rootTask.setCode("RT");
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
		task111.setTodo(25);
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
	}
	
	public void removeSampleTasks() throws DAOException, ModelException {
		removeRecursively(rootTask);
	}
	
	private void removeRecursively(Task task) throws DAOException, ModelException {
		// Récupération des taches filles
		Task[] subTasks = getModelMgr().getSubTasks(task);
		for (int i=subTasks.length-1; i>=0; i--) {
			Task subTask = subTasks[i];
			// Suppression des taches filles
			removeRecursively(subTask);
		}
		// Suppression de la tache
		getModelMgr().removeTask(task);
	}
	
	public void testTaskPath() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// Vérification des chemins
			String expectedPath = StringHelper.toHex(rootTask.getNumber()); 
			assertEquals(expectedPath, task1.getPath());
			assertEquals(expectedPath, task1.getPath());
			assertEquals(expectedPath + "01", task11.getPath());
			assertEquals(expectedPath, task2.getPath());
	
			// Vérification des numéros
			assertEquals(1, task1.getNumber());
			assertEquals(2, task2.getNumber());
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}
	
	public void testGetParent() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// La tache mère de 11 est 1 ?
			Task task11Parent = getModelMgr().getParentTask(task11);
			assertEquals(task1, task11Parent);
			
			// La tache mère de 1 est root ?
			Task task1Parent = getModelMgr().getParentTask(task1);
			assertEquals(rootTask, task1Parent);
	
			// La tache mère de root1 est null ?
			Task rootTaskParent = getModelMgr().getParentTask(rootTask);
			assertEquals(null, rootTaskParent);
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}

	public void testGetSubtasks() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// La tache root a-t-elle 2 filles ?
			Task[] rootTaskSubTasks = getModelMgr().getSubTasks(rootTask);
			assertEquals(2, rootTaskSubTasks.length);
			
			// La tache1 a-t-elle 1 fille ?
			Task[] task1SubTasks = getModelMgr().getSubTasks(task1);
			assertEquals(1, task1SubTasks.length);
			
			// La tache11 a-t-elle 2 fille ?
			Task[] task11SubTasks = getModelMgr().getSubTasks(task11);
			assertEquals(2, task11SubTasks.length);
	
			// La tache111 a-t-elle 0 filles ?
			Task[] task111SubTasks = getModelMgr().getSubTasks(task111);
			assertEquals(0, task111SubTasks.length);
	
			// La tache112 a-t-elle 0 filles ?
			Task[] task112SubTasks = getModelMgr().getSubTasks(task112);
			assertEquals(0, task112SubTasks.length);
	
			// La tache2 a-t-elle 0 filles ?
			Task[] task2SubTasks = getModelMgr().getSubTasks(task2);
			assertEquals(0, task2SubTasks.length);
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}

	public void testUpdate() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// Changement du nom de la tache
			rootTask.setName("New name");
			getModelMgr().updateTask(rootTask);
			
			// Chargement de la tache et control
			Task _rootTask = getModelMgr().getTask(rootTask.getId());
			assertEquals(rootTask.getName(), _rootTask.getName());
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}
	
	public void testMoveDown() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// Vérification des numéros des taches
			assertEquals((byte) 1, task1.getNumber());
			assertEquals((byte) 2, task2.getNumber());
	
			// Déplacement + vérification des nouveaux numéros
			getModelMgr().moveDownTask(task1);
			
			// Rechargement des taches
			task1 = getModelMgr().getTask(task1.getId());
			task2 = getModelMgr().getTask(task2.getId());
		
			// Controls
			assertEquals((byte) 2, task1.getNumber());
			assertEquals((byte) 1, task2.getNumber());
			assertEquals(1, getModelMgr().getSubTasksCount(task1.getId()));
			assertEquals(0, getModelMgr().getSubTasksCount(task2.getId()));
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}
	
	public void testMoveUp() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// Vérification des numéros des taches
			assertEquals((byte) 1, task1.getNumber());
			assertEquals((byte) 2, task2.getNumber());
	
			// Déplacement + vérification des nouveaux numéros
			getModelMgr().moveUpTask(task2);
			
			// Rechargement des taches
			task1 = getModelMgr().getTask(task1.getId());
			task2 = getModelMgr().getTask(task2.getId());
		
			// Controls
			assertEquals((byte) 2, task1.getNumber());
			assertEquals((byte) 1, task2.getNumber());
			assertEquals(1, getModelMgr().getSubTasksCount(task1.getId()));
			assertEquals(0, getModelMgr().getSubTasksCount(task2.getId()));
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}
	
	public void testMove() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// Vérification des numéros des taches
			assertEquals(1, getModelMgr().getSubTasksCount(task1.getId()));
			assertEquals(task1.getPath() + "01", task11.getPath());
			assertEquals(task11.getPath() + "01", task111.getPath());
			assertEquals(task11.getPath() + "01", task112.getPath());
			assertEquals((byte) 2, task112.getNumber());
	
			// Déplacement
			getModelMgr().moveTask(task111, task1);
			
			// Rechargement des taches qui ont été mises à jour
			task1 = getModelMgr().getTask(task1.getId());
			task2 = getModelMgr().getTask(task2.getId());
			task11 = getModelMgr().getTask(task11.getId());
			task111 = getModelMgr().getTask(task111.getId());
			task112 = getModelMgr().getTask(task112.getId());
		
			// Controls
			assertEquals(2, getModelMgr().getSubTasksCount(task1.getId()));
			assertEquals(task1.getPath() + "01", task11.getPath());
			assertEquals(task1.getPath() + "01", task111.getPath());
			assertEquals(task11.getPath() + "01", task112.getPath());
			assertEquals((byte) 1, task112.getNumber());
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}
	
	public void testTasksSum() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();

		// Récupération des sommes (sans critère de date)
		TaskSums taskSums = getModelMgr().getTaskSums(rootTask, null, null);
		assertEquals(
				task111.getBudget()
					+ task112.getBudget()
					+ task2.getBudget(),
				taskSums.getBudgetSum());
		assertEquals(
				task111.getInitiallyConsumed()
					+ task112.getInitiallyConsumed()
					+ task2.getInitiallyConsumed(),
				taskSums.getInitiallyConsumedSum());
		assertEquals(
				task111.getTodo()
					+ task112.getTodo()
					+ task2.getTodo(),
				taskSums.getTodoSum());
		
		assertEquals(
				0,
				taskSums.getContributionsNb());

		// Préparation de dates
		GregorianCalendar today = new GregorianCalendar();
		GregorianCalendar yesterday = new GregorianCalendar();
		yesterday.add(Calendar.DATE, -1);
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.add(Calendar.DATE, +1);
		
		// Ajout de deux contributions
		Duration duration = getFactory().newDuration();
		duration.setId(100);
		getModelMgr().createDuration(duration);
		Collaborator col = getModelMgr().createNewCollaborator();
		Contribution c1 = getFactory().newContribution();
		c1.setContributorId(col.getId());
		c1.setDate(yesterday);
		c1.setTaskId(task111.getId());
		c1.setDurationId(100);
		getModelMgr().createContribution(c1, false);
		Contribution c2 = getFactory().newContribution();
		c2.setContributorId(col.getId());
		c2.setDate(today);
		c2.setTaskId(task111.getId());
		c2.setDurationId(100);
		getModelMgr().createContribution(c2, false);
		
		// Calcul du consommé sur une période allant d'aujour'hui à aujourd'hui
		taskSums = getModelMgr().getTaskSums(rootTask, today, today);
		assertEquals(100, taskSums.getConsumedSum());
		assertEquals(1, taskSums.getContributionsNb());
		
		// Calcul du consommé & RAF sur une période allant d'hier à aujourd'hui
		taskSums = getModelMgr().getTaskSums(rootTask, yesterday, today);
		assertEquals(200, taskSums.getConsumedSum());
		assertEquals(2, taskSums.getContributionsNb());
		assertEquals(task111.getTodo() 
				+ task112.getTodo() 
				+ task2.getTodo(), taskSums.getTodoSum());
		
		// Calcul du consommé & RAF sur une période allant jusqu'à hier
		taskSums = getModelMgr().getTaskSums(rootTask, null, yesterday);
		assertEquals(100, taskSums.getConsumedSum());
		assertEquals(1, taskSums.getContributionsNb());
		assertEquals(task111.getTodo() 
				+ task112.getTodo() 
				+ task2.getTodo() 
				+ c2.getDurationId(), taskSums.getTodoSum());
		
		// Calcul du consommé & RAF sur une période débutant demain
		taskSums = getModelMgr().getTaskSums(rootTask, tomorrow, null);
		assertEquals(0, taskSums.getConsumedSum());
		assertEquals(0, taskSums.getContributionsNb());
		assertEquals(task111.getTodo() 
				+ task112.getTodo() 
				+ task2.getTodo(), taskSums.getTodoSum());

		// Suppression des données de test
		getModelMgr().removeContribution(c1, false);
		getModelMgr().removeContribution(c2, false);
		getModelMgr().removeDuration(duration);
		getModelMgr().removeCollaborator(col);
	
		// Suppression des taches de test
		removeSampleTasks();
	}

	public void testSearchTasks() throws DAOException, ModelException {
		// Création des taches de test
		createSampleTasks();
		try {
			// Recherche d'une tache avec le critère "dont le code est égal à..."
			TaskSearchFilter filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_CODE_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.IS_EQUAL_TO_CRITERIA_IDX);
			filter.setFieldValue("T1");
			Task[] tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);
			assertEquals(1, tasks.length);
			assertEquals("T1", tasks[0].getCode());
			
			// Recherche d'une tache avec le critère "dont le code commence par..."
			filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_CODE_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.STARTS_WITH_CRITERIA_IDX);
			filter.setFieldValue("T1");
			tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);
			assertEquals(4, tasks.length);
			assertEquals("T1", tasks[0].getCode());
			assertEquals("T11", tasks[1].getCode());
			assertEquals("T111", tasks[2].getCode());
			assertEquals("T112", tasks[3].getCode());

			// Recherche d'une tache avec le critère "dont le code finit par..."
			filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_CODE_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.ENDS_WITH_CRITERIA_IDX);
			filter.setFieldValue("11");
			tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);
			assertEquals(2, tasks.length);
			assertEquals("T11", tasks[0].getCode());
			assertEquals("T111", tasks[1].getCode());
		
			// Recherche d'une tache avec le critère "dont le code contient..."
			filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_CODE_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.CONTAINS_CRITERIA_IDX);
			filter.setFieldValue("T");
			tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);
			assertEquals(6, tasks.length);
			assertEquals("RT", tasks[0].getCode());
			assertEquals("T1", tasks[1].getCode());
			assertEquals("T11", tasks[2].getCode());
			assertEquals("T111", tasks[3].getCode());
			assertEquals("T112", tasks[4].getCode());
			assertEquals("T2", tasks[5].getCode());

			// Recherche d'une tache avec le critère "dont le code est égal à..."
			filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_NAME_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.IS_EQUAL_TO_CRITERIA_IDX);
			filter.setFieldValue("Task 1");
			tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);
			assertEquals(1, tasks.length);
			assertEquals("Task 1", tasks[0].getName());

			// Recherche d'une tache avec le critère "dont le code commence par..."
			filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_NAME_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.STARTS_WITH_CRITERIA_IDX);
			filter.setFieldValue("Task 1");
			tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);
			assertEquals(4, tasks.length);
			assertEquals("Task 1", tasks[0].getName());
			assertEquals("Task 11", tasks[1].getName());
			assertEquals("Task 111", tasks[2].getName());
			assertEquals("Task 112", tasks[3].getName());

			// Recherche d'une tache avec le critère "dont le code finit par..."
			filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_NAME_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.ENDS_WITH_CRITERIA_IDX);
			filter.setFieldValue("11");
			tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);
			assertEquals(2, tasks.length);
			assertEquals("Task 11", tasks[0].getName());
			assertEquals("Task 111", tasks[1].getName());
			
			// Recherche d'une tache avec le critère "dont le code contient..."
			filter = new TaskSearchFilter();
			filter.setFieldIndex(TaskSearchFilter.TASK_NAME_FIELD_IDX);
			filter.setCriteriaIndex(TaskSearchFilter.CONTAINS_CRITERIA_IDX);
			filter.setFieldValue("ask 1");
			tasks = getModelMgr().getTasks(filter);
			assertNotNull(tasks);

			assertEquals(4, tasks.length);
			assertEquals("Task 1", tasks[0].getName());
			assertEquals("Task 11", tasks[1].getName());
			assertEquals("Task 111", tasks[2].getName());
			assertEquals("Task 112", tasks[3].getName());
		}
		finally {
			// Suppression des taches de test
			removeSampleTasks();
		}
	}

	public void testMoveUpOrDownTask() throws DAOException, ModelException {
		createSampleTasks();
		try {
			// Création d'une tache avec 50 taches filles
			Task parentTask = getFactory().newTask();
			parentTask.setCode("PARENT");
			parentTask.setName("Parent task");
			parentTask = getModelMgr().createTask(rootTask, parentTask);
			for (int i=1; i<=50; i++) {
				Task newTask = getFactory().newTask();
				newTask.setCode("CD" + i);
				newTask.setName("Task # " + i);
				newTask = getModelMgr().createTask(parentTask, newTask);
			}
			// Reload pour rafraichissement du nombre de taches filles
			parentTask = getModelMgr().getTask(parentTask.getId());
			assertEquals(50, getModelMgr().getSubTasksCount(parentTask.getId()));
			
			// Déplacement impossible
			Task oneTask = getModelMgr().getTaskByCodePath("/RT/PARENT/CD34");
			assertEquals(34, oneTask.getNumber());
			try {
				getModelMgr().moveTaskUpOrDown(oneTask, 100);
				fail("Moving task to 200 is not possible!");
			}
			catch (ModelException e) {
				// Do nothing...
			}
			
			// Déplacement d'une tache vers le haut
			getModelMgr().moveTaskUpOrDown(oneTask, (byte) 3);
			Task oneTaskClone = getModelMgr().getTaskByCodePath("/RT/PARENT/CD34");
			assertEquals(oneTask.getId(), oneTaskClone.getId());
			assertEquals(oneTask.getName(), oneTaskClone.getName());
			assertEquals(3, oneTaskClone.getNumber());
			// Vérification du nombre d'enfants de la tache parent 
			assertEquals(50, getModelMgr().getSubTasksCount(parentTask.getId()));
			// Vérification des numéros des taches
			for (int i=1; i<=50; i++) {
				int taskNumber = getModelMgr().getTaskByCodePath("/RT/PARENT/CD" + i).getNumber();
				if (i<3)
					assertEquals(i, taskNumber);
				else if (i>=3 && i<34)
					assertEquals(i+1, taskNumber);
				else if (i==34)
					assertEquals(3, taskNumber);
				else 
					assertEquals(i, taskNumber);
			}
			
			// Déplacement inverse
			getModelMgr().moveTaskUpOrDown(oneTaskClone, (byte) 34);
			// Vérification des numéros des taches
			for (int i=1; i<=50; i++) {
				int taskNumber = getModelMgr().getTaskByCodePath("/RT/PARENT/CD" + i).getNumber();
				assertEquals(i, taskNumber);
			}
			
		}
		finally {
			removeSampleTasks();
		}
	}

}
