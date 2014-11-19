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
import org.activitymgr.core.impl.ModelMgrImpl;
import org.activitymgr.core.impl.dao.CollaboratorDAOImpl;
import org.activitymgr.core.impl.dao.ContributionDAOImpl;
import org.activitymgr.core.impl.dao.CoreDAOImpl;
import org.activitymgr.core.impl.dao.DurationDAOImpl;
import org.activitymgr.core.impl.dao.TaskDAOImpl;
import org.activitymgr.core.orm.DAOFactory;

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

	@Override
	protected void configure() {
		// Bind DAOs
		DAOFactory daoFactory = new DAOFactory();
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Collaborator>>(){})
	      .toInstance(daoFactory.getDAO(Collaborator.class));
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Task>>(){})
	      .toInstance(daoFactory.getDAO(Task.class));
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Duration>>(){})
	      .toInstance(daoFactory.getDAO(Duration.class));
		bind(new TypeLiteral<org.activitymgr.core.orm.IDAO<Contribution>>(){})
	      .toInstance(daoFactory.getDAO(Contribution.class));

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
