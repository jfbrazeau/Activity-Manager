package org.activitymgr.core.dao;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.Task;

public interface IDTOClassProvider {
	
	Class<? extends Task> getTaskClass();
	
	Class<? extends Duration> getDurationClass();
	
	Class<? extends Contribution> getContributionClass();
	
	Class<? extends Collaborator> getCollaboratorClass();

}