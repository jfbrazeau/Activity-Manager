package org.activitymgr.ui.web.logic.impl.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.model.IModelMgr;
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
