package org.activitymgr.core.dto;


public interface IDTOFactory {

	/**
	 * @return a new collaborator.
	 */
	Collaborator newCollaborator();
	
	/**
	 * @return a new duration.
	 */
	Duration newDuration();
	
	/**
	 * @return a new task.
	 */
	Task newTask();
	
	/**
	 * @return a new contribution.
	 */
	Contribution newContribution();

}
