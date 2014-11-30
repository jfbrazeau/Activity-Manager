package org.activitymgr.core.dao;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.DTOClassProvider;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.impl.dao.CollaboratorDAOImpl;
import org.activitymgr.core.impl.dao.ContributionDAOImpl;
import org.activitymgr.core.impl.dao.CoreDAOImpl;
import org.activitymgr.core.impl.dao.DTOFactoryImpl;
import org.activitymgr.core.impl.dao.DurationDAOImpl;
import org.activitymgr.core.impl.dao.TaskDAOImpl;
import org.activitymgr.core.orm.DAOFactory;
import org.activitymgr.core.orm.IDAO;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class CoreDAOModule implements Module {

	private DTOClassProvider dtoClassProvider;
	
	public CoreDAOModule(DTOClassProvider dtoClassProvider) {
		this.dtoClassProvider = dtoClassProvider;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void configure(Binder binder) {
		// Bind DAOs
		DAOFactory daoFactory = new DAOFactory();
		binder.bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Collaborator>>(){})
	      .toInstance((IDAO<Collaborator>) daoFactory.getDAO(dtoClassProvider.getCollaboratorClass()));
		binder.bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Task>>(){})
	      .toInstance((IDAO<Task>) daoFactory.getDAO(dtoClassProvider.getTaskClass()));
		binder.bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Duration>>(){})
	      .toInstance((IDAO<Duration>) daoFactory.getDAO(dtoClassProvider.getDurationClass()));
		binder.bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Contribution>>(){})
	      .toInstance((IDAO<Contribution>) daoFactory.getDAO(dtoClassProvider.getContributionClass()));

		// Bind wrappers
		binder.bind(ICollaboratorDAO.class)
	      .to(CollaboratorDAOImpl.class).in(Singleton.class);;
	    binder.bind(ITaskDAO.class)
	      .to(TaskDAOImpl.class).in(Singleton.class);;
	    binder.bind(IDurationDAO.class)
	      .to(DurationDAOImpl.class).in(Singleton.class);;
	    binder.bind(IContributionDAO.class)
	      .to(ContributionDAOImpl.class).in(Singleton.class);;

		// Bind core DAO & ModelManager
	    binder.bind(IDTOFactory.class).to(DTOFactoryImpl.class).in(Singleton.class);
	    binder.bind(ICoreDAO.class).to(CoreDAOImpl.class).in(Singleton.class);
	}
	
}
