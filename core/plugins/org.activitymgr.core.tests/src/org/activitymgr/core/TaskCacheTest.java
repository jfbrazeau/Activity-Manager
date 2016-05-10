package org.activitymgr.core;

import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.dao.TaskDAOCache;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;

import com.google.inject.Inject;

public class TaskCacheTest extends AbstractModelTestCase {
	
	@Inject
	private ITaskDAO taskDAO;

	/** Taches de test */
	private Task rootTask;
	private Task task1;
	private Task task11;
	private Task task111;
	private Task task112;
	private Task task2;

	public void testCache() throws ModelException {
		createSampleTasks();
		try {
			TaskDAOCache cache = new TaskDAOCache(taskDAO);
			
			// Unknown code path
			assertNull(cache.getByCodePath("/Unknown/Path"));
			
			// Regular code path
			Task byCodePath = cache.getByCodePath("/RT/T1/T11/T112");
			assertNotNull(byCodePath);
			assertEquals(task112.getId(), byCodePath.getId());
			
			// Unknown full path
			assertNull(cache.getByFullPath("0099"));
			
			// Regular full path
			Task byPath = cache.getByFullPath(task112.getFullPath());
			assertNotNull(byPath);
			assertEquals(task112.getId(), byPath.getId());

			// Test get parent
			Task parent = cache.getParent(task112);
			assertNotNull(parent);
			assertEquals(task11.getId(), parent.getId());
			
			// Parent of root task
			assertNull(cache.getParent(rootTask));
			
		}
		finally {
			removeSampleTasks();
		}
	}
	
	private void createSampleTasks() throws ModelException {
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
	
	public void removeSampleTasks() throws ModelException {
		removeRecursively(rootTask);
	}
	
	private void removeRecursively(Task task) throws ModelException {
		// Récupération des taches filles
		Task[] subTasks = getModelMgr().getSubTasks(task.getId());
		for (int i=subTasks.length-1; i>=0; i--) {
			Task subTask = subTasks[i];
			// Suppression des taches filles
			removeRecursively(subTask);
		}
		// Suppression de la tache
		getModelMgr().removeTask(task);
	}
	
}
