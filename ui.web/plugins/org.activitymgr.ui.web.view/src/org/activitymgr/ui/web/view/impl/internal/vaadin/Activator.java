package org.activitymgr.ui.web.view.impl.internal.vaadin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;

import org.activitymgr.ui.web.view.impl.internal.ViewModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.google.gwt.dev.util.collect.HashSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

@SuppressWarnings("rawtypes")
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	public static final String BUNDLE_ID = "org.activitymgr.ui.web.view";

	private static final String UI_PROVIDER_PARAM = "UIProvider";

	private static final String WIDGETSET_PARAM = "widgetset";

	private static final String PRODUCTION_MODE_PARAM = "productionMode";

	private static final List<Class<?>> SERVICE_CLASSES = Arrays
			.asList(new Class<?>[] { IExtensionRegistry.class,
					HttpService.class /* , HttpContextExtensionService.class */});

	private BundleContext context;

	private Map<Class<?>, ServiceTracker<?, ?>> serviceTrackers = new HashMap<Class<?>, ServiceTracker<?, ?>>();

	private IExtensionRegistry extensionRegistryService;

	private boolean initialized = false;

	private HttpService httpService;

	private static Activator singleton = null;
	
	private Injector injector;

	public Activator() {
		// Save singleton
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
			extensionRegistryService = (IExtensionRegistry) service;
		}
		if (service instanceof HttpService && httpService == null) {
			httpService = (HttpService) service;
		}
		if (!initialized && extensionRegistryService != null
				&& httpService != null) {
			// Prevents from initializing several times
			initialized = true;
			init();
		}
		return null;
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


	private void init() {
		List<AbstractModule> modules = new ArrayList<AbstractModule>();
		IConfigurationElement[] cfgs = getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.additionalModules");
		for (IConfigurationElement cfg : cfgs) {
			try {
				modules.add((AbstractModule) cfg.createExecutableExtension("class"));
			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
		}
		// Activity Manager module can be overriden
		Module module = Modules.override(new ViewModule()).with(modules);
		// Injector creation
		injector = Guice.createInjector(module);

		Properties props = new Properties();
		props.put(PRODUCTION_MODE_PARAM,
				"true");
		//props.put(WIDGETSET_PARAM, widgetset);
		props.put(UI_PROVIDER_PARAM, OSGiUIProvider.class.getName());

		// Retrieve bundles that may contain resources
		Set<Bundle> resourceProviderBundles = new HashSet<Bundle>();
		for (Bundle bundle : context.getBundles()) {
			if (bundle.getResource("VAADIN") != null) {
				System.out.println("Register vaadin contributions from " + bundle);
				resourceProviderBundles.add(bundle);
			}
		}
		
		// Register application bundle
		Exception exception = null;
		try {
			httpService.registerServlet(
					"/",
					new ActivityMgrServlet(),
					props,
					new OSGiUIHttpContext(httpService
							.createDefaultHttpContext(), resourceProviderBundles));
		} catch (ServletException e) {
			exception = e;
		} catch (NamespaceException e) {
			exception = e;
		}
		if (exception != null) {
			logError("Error while registering Vaadin UI", exception);
		}
	}

	public void logError(String message, Throwable exception) {
		ILog log = Platform.getLog(context.getBundle());
		log.log(new Status(IStatus.ERROR, BUNDLE_ID, message, exception));

	}

	public void logError(String message) {
		ILog log = Platform.getLog(context.getBundle());
		log.log(new Status(IStatus.ERROR, BUNDLE_ID, message));

	}

	public void logWarn(String message) {
		ILog log = Platform.getLog(context.getBundle());
		log.log(new Status(IStatus.WARNING, BUNDLE_ID, message));

	}

	public Injector getInjector() {
		return injector;
	}

}
