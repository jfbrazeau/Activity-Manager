package org.activitymgr.core.dao;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
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
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class CoreDAOModule implements Module {
	
	@SuppressWarnings("unchecked")
	@Override
	public void configure(Binder binder) {
		// Bind DAOs
		final DAOFactory daoFactory = new DAOFactory();
		binder.bind(new TypeLiteral<IDAO<Collaborator>>() {
		}).toProvider(new Provider<IDAO<Collaborator>>() {
			@Inject(optional = true)
			IDTOClassProvider dtoClassProvider;
			@Override
			public IDAO<Collaborator> get() {
				return (IDAO<Collaborator>) daoFactory
						.getDAO(dtoClassProvider == null ? Collaborator.class
								: dtoClassProvider.getCollaboratorClass());
			}
		}).in(Singleton.class);
		binder.bind(new TypeLiteral<IDAO<Task>>() {
		}).toProvider(new Provider<IDAO<Task>>() {
			@Inject(optional = true)
			IDTOClassProvider dtoClassProvider;
			@Override
			public IDAO<Task> get() {
				return (IDAO<Task>) daoFactory
						.getDAO(dtoClassProvider == null ? Task.class
								: dtoClassProvider.getTaskClass());
			}
		}).in(Singleton.class);
		binder.bind(new TypeLiteral<IDAO<Duration>>() {
		}).toProvider(new Provider<IDAO<Duration>>() {
			@Inject(optional = true)
			IDTOClassProvider dtoClassProvider;
			@Override
			public IDAO<Duration> get() {
				return (IDAO<Duration>) daoFactory
						.getDAO(dtoClassProvider == null ? Duration.class
								: dtoClassProvider.getDurationClass());
			}
		}).in(Singleton.class);
		binder.bind(new TypeLiteral<IDAO<Contribution>>() {
		}).toProvider(new Provider<IDAO<Contribution>>() {
			@Inject(optional = true)
			IDTOClassProvider dtoClassProvider;
			@Override
			public IDAO<Contribution> get() {
				return (IDAO<Contribution>) daoFactory
						.getDAO(dtoClassProvider == null ? Contribution.class
								: dtoClassProvider.getContributionClass());
			}
		}).in(Singleton.class);
		
		// Bind DAO wrappers
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
