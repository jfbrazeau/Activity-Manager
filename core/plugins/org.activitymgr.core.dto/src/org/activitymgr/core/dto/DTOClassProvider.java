package org.activitymgr.core.dto;

public class DTOClassProvider {
	
	public Class<? extends Task> getTaskClass() {
		return Task.class;
	}
	
	public Class<? extends Duration> getDurationClass() {
		return Duration.class;
	}
	
	public Class<? extends Contribution> getContributionClass() {
		return Contribution.class;
	}
	
	public Class<? extends Collaborator> getCollaboratorClass() {
		return Collaborator.class;
	}
}