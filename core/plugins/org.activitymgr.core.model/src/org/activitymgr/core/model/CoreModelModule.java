package org.activitymgr.core.model;

import org.activitymgr.core.dao.CoreDAOModule;
import org.activitymgr.core.model.impl.ModelMgrImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class CoreModelModule extends AbstractModule {

//	public static interface IPostInjectionListener {
//		
//		void afterInjection() throws Exception;
//
//	}
//
	@Override
	protected void configure() {
		// Bind DAO layer
		CoreDAOModule daoModule = new CoreDAOModule();
		daoModule.configure(binder());
		
		// Bind core ModelManager
		bind(IModelMgr.class).to(ModelMgrImpl.class).in(Singleton.class);
		
		// Bind post injection listeners
//		bindListener(Matchers.any(), new TypeListener() {
//			@Override
//			public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
//				encounter.register(new InjectionListener<I>() {
//					@Override
//					public void afterInjection(I injectee) {
//						if (injectee instanceof IPostInjectionListener) {
//							try {
//								((IPostInjectionListener) injectee).afterInjection();
//							} catch (Exception e) {
//								throw new IllegalStateException("Post Guice injection exception", e);
//							}
//						}
//					}
//				});
//				
//			}
//		});
	}
	
}
