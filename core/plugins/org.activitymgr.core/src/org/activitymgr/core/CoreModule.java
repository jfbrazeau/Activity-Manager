package org.activitymgr.core;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
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

		bind(ICoreDAO.class).to(CoreDAOImpl.class);
		bind(IModelMgr.class).to(ModelMgrImpl.class);
		
	}
	
}
