package org.activitymgr.core;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;

public interface IBeanFactory {

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
