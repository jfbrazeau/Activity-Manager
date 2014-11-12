package org.activitymgr.core;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.impl.AbstractORMDAOWrapperImpl.CollaboratorDAOWrapper;
import org.activitymgr.core.impl.AbstractORMDAOWrapperImpl.ContributionDAOWrapper;
import org.activitymgr.core.impl.AbstractORMDAOWrapperImpl.DurationDAOWrapper;
import org.activitymgr.core.impl.AbstractORMDAOWrapperImpl.TaskDAOWrapper;
import org.activitymgr.core.impl.CoreDAOImpl;
import org.activitymgr.core.impl.ModelMgrImpl;
import org.activitymgr.core.orm.DAOFactory;
import org.activitymgr.core.orm.IDAO;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class CoreModule extends AbstractModule {

	@Override
	protected void configure() {
		// Bind DAOs
		DAOFactory daoFactory = new DAOFactory();
		bind(new TypeLiteral<IDAO<Collaborator>>(){})
	      .toInstance(daoFactory.getDAO(Collaborator.class));
		bind(new TypeLiteral<IDAO<Task>>(){})
	      .toInstance(daoFactory.getDAO(Task.class));
		bind(new TypeLiteral<IDAO<Duration>>(){})
	      .toInstance(daoFactory.getDAO(Duration.class));
		bind(new TypeLiteral<IDAO<Contribution>>(){})
	      .toInstance(daoFactory.getDAO(Contribution.class));

		// Bind wrappers
		bind(new TypeLiteral<IORMDAOWrapper<Collaborator>>(){})
	      .to(CollaboratorDAOWrapper.class);
		bind(new TypeLiteral<IORMDAOWrapper<Task>>(){})
	      .to(TaskDAOWrapper.class);
		bind(new TypeLiteral<IORMDAOWrapper<Duration>>(){})
	      .to(DurationDAOWrapper.class);
		bind(new TypeLiteral<IORMDAOWrapper<Contribution>>(){})
	      .to(ContributionDAOWrapper.class);

		// Bind core DAO & ModelManager
		bind(ICoreDAO.class).to(CoreDAOImpl.class);
		bind(IModelMgr.class).to(ModelMgrImpl.class);
		
	}
	
}
