package jfb.tools.activitymgr.core.beans;

/**
 * Contains the contributions of a given task on a given period.
 * @author jbrazeau
 */
public class TaskContributions {
	
	/** The task */
	private Task task;
	
	/** The task code path */
	private String taskCodePath;
	
	/** The contributions */
	private Contribution[] contributions;

	/**
	 * @return the task
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * @param task the task to set
	 */
	public void setTask(Task task) {
		this.task = task;
	}

	/**
	 * @return the contributions
	 */
	public Contribution[] getContributions() {
		return contributions;
	}

	/**
	 * @param contributions the contributions to set
	 */
	public void setContributions(Contribution[] contributions) {
		this.contributions = contributions;
	}

	/**
	 * @return the task code path
	 */
	public String getTaskCodePath() {
		return taskCodePath;
	}

	/**
	 * @param taskCodePath the new task code path
	 */
	public void setTaskCodePath(String taskCodePath) {
		this.taskCodePath = taskCodePath;
	}

}