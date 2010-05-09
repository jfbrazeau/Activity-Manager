package jfb.tst.tools.activitymgr.core;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.beans.TaskSums;
import jfb.tools.activitymgr.core.util.StringHelper;
import jfb.tst.tools.activitymgr.AbstractModelTestCase;

public class TaskTest extends AbstractModelTestCase {

	/** Taches de test */
	private Task rootTask;
	private Task task1;
	private Task task11;
	private Task task111;
	private Task task112;
	private Task task2;

	public void testCreateRootTask() throws DbException, ModelException {
		// Création des tâches de test
		Task task = new Task();
		// Génération d'un code inutilisé
		task.setCode(String.valueOf(System.currentTimeMillis()).substring(8));
		task.setName("Root task");
		task = ModelMgr.createTask(null, task);
		
		// Vérification de la génération du numéro
		assertTrue(task.getNumber()>0);
		
		// Relecture de la tâche en base et comparaison
		long taskId = task.getId();
		Task otherTask = ModelMgr.getTask(taskId);
		assertEquals(task, otherTask); // Seuls les identifiants sont comparés
		assertEquals(task.getCode(), otherTask.getCode());
		assertEquals(task.getName(), otherTask.getName());
		assertEquals(task.getNumber(), otherTask.getNumber());
		
		// Suppression
		ModelMgr.removeTask(task);
		
		// Tentative de relecture (l'objet ne doit plus exister)
		assertNull(ModelMgr.getTask(taskId));
	}
	
	private void createSampleTasks() throws DbException, ModelException {
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
	}
	
	public void removeSampleTasks() throws DbException, ModelException {
		removeRecursively(rootTask);
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
	
	public void testTaskPath() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();
		
		// Vérification des chemins
		String expectedPath = StringHelper.toHex(rootTask.getNumber()); 
		assertEquals(expectedPath, task1.getPath());
		assertEquals(expectedPath, task1.getPath());
		assertEquals(expectedPath + "01", task11.getPath());
		assertEquals(expectedPath, task2.getPath());

		// Vérification des numéros
		assertEquals(1, task1.getNumber());
		assertEquals(2, task2.getNumber());
		
		// Suppression des taches de test
		removeSampleTasks();
	}
	
	public void testGetParent() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();

		// La tache mère de 11 est 1 ?
		Task task11Parent = ModelMgr.getParentTask(task11);
		assertEquals(task1, task11Parent);
		
		// La tache mère de 1 est root ?
		Task task1Parent = ModelMgr.getParentTask(task1);
		assertEquals(rootTask, task1Parent);

		// La tache mère de root1 est null ?
		Task rootTaskParent = ModelMgr.getParentTask(rootTask);
		assertEquals(null, rootTaskParent);

		// Suppression des taches de test
		removeSampleTasks();
	}

	public void testGetSubtasks() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();
		
		// La tache root a-t-elle 2 filles ?
		Task[] rootTaskSubTasks = ModelMgr.getSubtasks(rootTask);
		assertEquals(2, rootTaskSubTasks.length);
		
		// La tache1 a-t-elle 1 fille ?
		Task[] task1SubTasks = ModelMgr.getSubtasks(task1);
		assertEquals(1, task1SubTasks.length);
		
		// La tache11 a-t-elle 2 fille ?
		Task[] task11SubTasks = ModelMgr.getSubtasks(task11);
		assertEquals(2, task11SubTasks.length);

		// La tache111 a-t-elle 0 filles ?
		Task[] task111SubTasks = ModelMgr.getSubtasks(task111);
		assertEquals(0, task111SubTasks.length);

		// La tache112 a-t-elle 0 filles ?
		Task[] task112SubTasks = ModelMgr.getSubtasks(task112);
		assertEquals(0, task112SubTasks.length);

		// La tache2 a-t-elle 0 filles ?
		Task[] task2SubTasks = ModelMgr.getSubtasks(task2);
		assertEquals(0, task2SubTasks.length);

		// Suppression des taches de test
		removeSampleTasks();
	}

	public void testUpdate() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();

		// Changement du nom de la tache
		rootTask.setName("New name");
		ModelMgr.updateTask(rootTask);
		
		// Chargement de la tache et control
		Task _rootTask = ModelMgr.getTask(rootTask.getId());
		assertEquals(rootTask.getName(), _rootTask.getName());
		
		// Suppression des taches de test
		removeSampleTasks();
	}
	
	public void testMoveDown() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();

		// Vérification des numéros des taches
		assertEquals((byte) 1, task1.getNumber());
		assertEquals((byte) 2, task2.getNumber());

		// Déplacement + vérification des nouveaux numéros
		ModelMgr.moveDownTask(task1);
		
		// Rechargement des taches
		task1 = ModelMgr.getTask(task1.getId());
		task2 = ModelMgr.getTask(task2.getId());
	
		// Controls
		assertEquals((byte) 2, task1.getNumber());
		assertEquals((byte) 1, task2.getNumber());
		assertEquals(1, task1.getSubTasksCount());
		assertEquals(0, task2.getSubTasksCount());
		
		// Suppression des taches de test
		removeSampleTasks();
	}
	
	public void testMoveUp() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();

		// Vérification des numéros des taches
		assertEquals((byte) 1, task1.getNumber());
		assertEquals((byte) 2, task2.getNumber());

		// Déplacement + vérification des nouveaux numéros
		ModelMgr.moveUpTask(task2);
		
		// Rechargement des taches
		task1 = ModelMgr.getTask(task1.getId());
		task2 = ModelMgr.getTask(task2.getId());
	
		// Controls
		assertEquals((byte) 2, task1.getNumber());
		assertEquals((byte) 1, task2.getNumber());
		assertEquals(1, task1.getSubTasksCount());
		assertEquals(0, task2.getSubTasksCount());
		
		// Suppression des taches de test
		removeSampleTasks();
	}
	
	public void testMove() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();

		// Vérification des numéros des taches
		assertEquals(1, task1.getSubTasksCount());
		assertEquals(task1.getPath() + "01", task11.getPath());
		assertEquals(task11.getPath() + "01", task111.getPath());
		assertEquals(task11.getPath() + "01", task112.getPath());
		assertEquals((byte) 2, task112.getNumber());

		// Déplacement
		ModelMgr.moveTask(task111, task1);
		
		// Rechargement des taches qui ont été mises à jour
		task1 = ModelMgr.getTask(task1.getId());
		task2 = ModelMgr.getTask(task2.getId());
		task11 = ModelMgr.getTask(task11.getId());
		task111 = ModelMgr.getTask(task111.getId());
		task112 = ModelMgr.getTask(task112.getId());
	
		// Controls
		assertEquals(2, task1.getSubTasksCount());
		assertEquals(task1.getPath() + "01", task11.getPath());
		assertEquals(task1.getPath() + "01", task111.getPath());
		assertEquals(task11.getPath() + "01", task112.getPath());
		assertEquals((byte) 1, task112.getNumber());
		
		// Suppression des taches de test
		removeSampleTasks();
	}
	
	public void testTasksSum() throws DbException, ModelException {
		// Création des taches de test
		createSampleTasks();
		
		// Récupération des sommes
		TaskSums taskSums = ModelMgr.getTaskSums(rootTask);
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
		// Suppression des taches de test
		removeSampleTasks();
	}

}
