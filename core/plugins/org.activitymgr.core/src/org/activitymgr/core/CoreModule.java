package org.activitymgr.core;

import org.activitymgr.core.impl.DbMgrImpl;
import org.activitymgr.core.impl.ModelMgrImpl;

import com.google.inject.AbstractModule;

public class CoreModule extends AbstractModule {

	@Override
	protected void configure() {
		// TODO remove
		// http://code.google.com/p/google-guice/wiki/AOP
		// http://code.google.com/p/google-guice/wiki/GuicePersist
		// http://blog.xebia.fr/2009/04/15/google-guice-les-bases-de-linjection-de-dependances/
		bind(IDbMgr.class).to(DbMgrImpl.class);
		bind(IModelMgr.class).to(ModelMgrImpl.class);
	}
	
}
