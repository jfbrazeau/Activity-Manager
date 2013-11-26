package org.activitymgr.ui.web.logic.impl.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.IModelMgr;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@SuppressWarnings("rawtypes")
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	private static final List<Class<?>> SERVICE_CLASSES = Arrays
			.asList(new Class<?>[] { IExtensionRegistry.class });

	private BundleContext context;

	private Map<Class<?>, ServiceTracker<?, ?>> serviceTrackers = new HashMap<Class<?>, ServiceTracker<?, ?>>();

	private IExtensionRegistry extensionRegistryService;

	private IModelMgr modelMgr;

	private BasicDataSource datasource;

	private static Activator singleton = null;
	
	public Activator() {
		singleton = this;
	}
	
	public static Activator getDefault() {
		return singleton;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		for (Class<?> cl : SERVICE_CLASSES) {
			@SuppressWarnings({ "unchecked" })
			ServiceTracker st = new ServiceTracker(context, cl.getName(), this);
			serviceTrackers.put(cl, st);
			st.open();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <TYPE> Class<TYPE> loadClass(String bundleId, String className) throws ClassNotFoundException {
		for (Bundle b : context.getBundles()) {
			if (bundleId.equals(b.getSymbolicName())) {
				return (Class<TYPE>) b.loadClass(className);
			}
		}
		return null;
	}

	protected IModelMgr getModelMgr() {
		return modelMgr;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.context = null;
		for (ServiceTracker<?, ?> st : serviceTrackers.values()) {
			st.close();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object addingService(ServiceReference reference) {
		Object service = context.getService(reference);
		if (service instanceof IExtensionRegistry
				&& extensionRegistryService == null) {
			registerExtensionRegistryService((IExtensionRegistry) service);
		}
		return null;
	}

	private void registerExtensionRegistryService(IExtensionRegistry registry) {
		extensionRegistryService = registry;

		// TODO remove
//		// Once the registry is registered, ModelMgr implementation 
//		// can be created
//		// TODO externalize connection parameters
//		// Create the datasource
//		String jdbcDriver = "com.mysql.jdbc.Driver";
//		String jdbcUrl = "jdbc:mysql://localhost:3306/taskmgr_db";
//		String jdbcUser = "taskmgr_user";
//		String jdbcPassword = "secret";
//		datasource = new BasicDataSource();
//		datasource.setDriverClassName(jdbcDriver);
//		datasource.setUrl(jdbcUrl);
//		datasource.setUsername(jdbcUser);
//		datasource.setPassword(jdbcPassword);
//		datasource.setDefaultAutoCommit(false);
//		
//		// Create Guice injector
//		final ThreadLocal<DbTransaction> dbTxs = new ThreadLocal<DbTransaction>();
//		Injector injector = Guice.createInjector(new CoreModule(),
//				new AbstractModule() {
//					@Override
//					protected void configure() {
//						bind(DbTransaction.class).toProvider(
//								new Provider<DbTransaction>() {
//									@Override
//									public DbTransaction get() {
//										return dbTxs.get();
//									}
//								});
//					}
//				});
//		// Creates a new model manager wrapper (managing the transaction)
//		final IModelMgr wrappedModelMgr = injector.getInstance(IModelMgr.class);
//		modelMgr = (IModelMgr) Proxy.newProxyInstance(
//				Activator.class.getClassLoader(),
//				new Class<?>[] { IModelMgr.class }, new InvocationHandler() {
//					@Override
//					public Object invoke(Object proxy, Method method,
//							Object[] args) throws Throwable {
//						DbTransaction tx = null;
//						try {
//							// Open the transaction
//							tx = new DbTransaction(datasource.getConnection());
//							dbTxs.set(tx);
//							// Call the real model manager
//							Object result = method.invoke(wrappedModelMgr, args);
//							// Commit the transaction
//							tx.getConnection().commit();
//							return result;
//						} catch (InvocationTargetException t) {
//							// Rollback the transaction in case of failure
//							tx.getConnection().rollback();
//							throw t.getCause();
//						} finally {
//							// Release the transaction
//							dbTxs.remove();
//							tx.getConnection().close();
//						}
//					}
//				});
	}
	
	@Override
	public void modifiedService(ServiceReference reference,
			Object service) {
	}

	@Override
	public void removedService(ServiceReference reference,
			Object service) {
		context.ungetService(reference);
		if (service instanceof IExtensionRegistry){
			try {
				modelMgr = null;
				datasource.close();
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public IExtensionRegistry getExtensionRegistryService() {
		return extensionRegistryService;
	}

}
