package org.activitymgr.core.impl;

import org.activitymgr.core.IBeanFactory;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dao.IDurationDAO;
import org.activitymgr.core.dao.ITaskDAO;

import com.google.inject.Inject;

public class BeanFactoryImpl implements IBeanFactory {

	/** Collaborators DAO */
	@Inject
	private ICollaboratorDAO collaboratorDAO;

	/** Tasks DAO */
	@Inject
	private ITaskDAO taskDAO;

	/** Durations DAO */
	@Inject
	private IDurationDAO durationDAO;

	/** Contributions DAO */
	@Inject
	private IContributionDAO contributionDAO;

	@Override
	public Collaborator newCollaborator() {
		return collaboratorDAO.newInstance();
	}

	@Override
	public Duration newDuration() {
		return durationDAO.newInstance();
	}

	@Override
	public Task newTask() {
		return taskDAO.newInstance();
	}

	@Override
	public Contribution newContribution() {
		return contributionDAO.newInstance();
	}


}
