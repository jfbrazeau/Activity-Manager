package org.activitymgr.core.impl.dao;

import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dao.IDurationDAO;
import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;

import com.google.inject.Inject;

public class DTOFactoryImpl implements IDTOFactory {

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
