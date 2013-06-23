package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelMgr;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
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

	private ModelMgr modelMgr;

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

	protected ModelMgr getModelMgr() {
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
		// Once the registry is registered, ModelMgr implementation 
		// can be created
		// TODO externalize connection parameters
		try {
			IConfigurationElement[] cfgs = registry.getConfigurationElementsFor("org.activitymgr.ui.web.logic.modelMgrImpl");
			ModelMgr modelMgr = 
					cfgs.length > 0 ? 
							(ModelMgr) cfgs[0].createExecutableExtension("class") 
							: new ModelMgr();
			modelMgr.initialize("com.mysql.jdbc.Driver",
					"jdbc:mysql://localhost:3306/taskmgr_db", "taskmgr_user",
					"secret");
			// If model manager intializaation is successful, the instance is kept
			this.modelMgr = modelMgr;
		}
		catch (DbException e) {
			// TODO create a log
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO create a log
			e.printStackTrace();
		}
	}
	
	@Override
	public void modifiedService(ServiceReference reference,
			Object service) {
	}

	@Override
	public void removedService(ServiceReference reference,
			Object service) {
		context.ungetService(reference);
	}

	public IExtensionRegistry getExtensionRegistryService() {
		return extensionRegistryService;
	}

}
