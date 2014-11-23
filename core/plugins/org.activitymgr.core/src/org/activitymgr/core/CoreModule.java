package org.activitymgr.core;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dao.ICoreDAO;
import org.activitymgr.core.dao.IDurationDAO;
import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.impl.BeanFactoryImpl;
import org.activitymgr.core.impl.ModelMgrImpl;
import org.activitymgr.core.impl.dao.CollaboratorDAOImpl;
import org.activitymgr.core.impl.dao.ContributionDAOImpl;
import org.activitymgr.core.impl.dao.CoreDAOImpl;
import org.activitymgr.core.impl.dao.DurationDAOImpl;
import org.activitymgr.core.impl.dao.TaskDAOImpl;
import org.activitymgr.core.orm.DAOFactory;
import org.activitymgr.core.orm.IDAO;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class CoreModule extends AbstractModule {

	public static interface IPostInjectionListener {
		
		void afterInjection() throws Exception;

	}

	public static class BeanClassProvider {
		
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
	
	private BeanClassProvider beanClassProvider;
	
	public CoreModule(BeanClassProvider beanClassProvider) {
		this.beanClassProvider = beanClassProvider;
	}
	
	public CoreModule() {
		this(new BeanClassProvider());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void configure() {
		// Bind DAOs
		DAOFactory daoFactory = new DAOFactory();
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Collaborator>>(){})
	      .toInstance((IDAO<Collaborator>) daoFactory.getDAO(beanClassProvider.getCollaboratorClass()));
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Task>>(){})
	      .toInstance((IDAO<Task>) daoFactory.getDAO(beanClassProvider.getTaskClass()));
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Duration>>(){})
	      .toInstance((IDAO<Duration>) daoFactory.getDAO(beanClassProvider.getDurationClass()));
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Contribution>>(){})
	      .toInstance((IDAO<Contribution>) daoFactory.getDAO(beanClassProvider.getContributionClass()));

		// Bind wrappers
		bind(ICollaboratorDAO.class)
	      .to(CollaboratorDAOImpl.class).in(Singleton.class);;
		bind(ITaskDAO.class)
	      .to(TaskDAOImpl.class).in(Singleton.class);;
		bind(IDurationDAO.class)
	      .to(DurationDAOImpl.class).in(Singleton.class);;
		bind(IContributionDAO.class)
	      .to(ContributionDAOImpl.class).in(Singleton.class);;

		// Bind core DAO & ModelManager
	    bind(IBeanFactory.class).to(BeanFactoryImpl.class).in(Singleton.class);
	    bind(ICoreDAO.class).to(CoreDAOImpl.class).in(Singleton.class);
		bind(IModelMgr.class).to(ModelMgrImpl.class).in(Singleton.class);
		
		// Bind post injection listeners
		bindListener(Matchers.any(), new TypeListener() {
			@Override
			public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
				encounter.register(new InjectionListener<I>() {
					@Override
					public void afterInjection(I injectee) {
						if (injectee instanceof IPostInjectionListener) {
							System.out.println("" + injectee + ".afterInjection()");
							try {
								((IPostInjectionListener) injectee).afterInjection();
							} catch (Exception e) {
								throw new IllegalStateException("Post Guice injection exception", e);
							}
						}
					}
				});
				
			}
		});
	}
	
}
