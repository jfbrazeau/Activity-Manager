package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IButtonLogic;
import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.ILogicContext;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.impl.event.EventBusImpl;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;

public class RootLogicImpl implements IRootLogic {

	private Injector userInjector;
	
	private IRootLogic.View view;
	
	public RootLogicImpl(IRootLogic.View rootView, Injector mainInjector) {
		userInjector = mainInjector.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IEventBus.class).to(EventBusImpl.class).in(Singleton.class);
				bind(ILogicContext.class).to(LogicContextImpl.class).in(Singleton.class);
				
			}
		});

		// View registration
		this.view = rootView;
		view.registerLogic(this);

		// Model manager retrieval
		// Create authentication logic
		getView().setContentView(new AuthenticationLogicImpl(this).getView());
		
		// Event listeners registration
		IEventBus eventBus = userInjector.getInstance(IEventBus.class);
		eventBus.register(CallbackExceptionEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				// If an error occurs in a view callback, it shows the error to the user
				handleError(getView(), ((CallbackExceptionEvent) event).getException());
			}
		});
		eventBus.register(ConnectedCollaboratorEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				// Create the tab container
				TabFolderLogicImpl tabFolderLogic = new TabFolderLogicImpl(RootLogicImpl.this);
				getView().setContentView(tabFolderLogic.getView());
				// Iterate over the provided tabs and create it
				IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.tabLogic");
				List<IConfigurationElement> cfgList = new ArrayList<IConfigurationElement>(Arrays.asList(cfgs));
				Collections.reverse(cfgList);
				for (IConfigurationElement cfg : cfgList) {
					String tabId = cfg.getAttribute("id");
					String tabName = cfg.getAttribute("label");
					// Check user access
					if (getContext().getAccessManager().hasAccessToTab(getContext().getConnectedCollaborator(), tabName)) {
						try {
							Class<AbstractTabLogicImpl<?>> tabLogicClass = Activator.getDefault().<AbstractTabLogicImpl<?>>loadClass(cfg.getContributor().getName(), cfg.getAttribute("class"));
							AbstractTabLogicImpl<?> tabLogic = getContext().newExtensionInstance(tabLogicClass, AbstractLogicImpl.class, tabFolderLogic);
							tabFolderLogic.addTab(tabName, tabLogic);
							addButtons(tabLogic, tabId);
						} catch (ClassNotFoundException e) {
							throw new IllegalStateException(e);
						}
					}
				}
				
			}

		});
	}
	
	private void addButtons(AbstractTabLogicImpl<?> tabLogic, String tabId) throws InvalidRegistryObjectException, ClassNotFoundException {
		// Create actions
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.button");
		for (IConfigurationElement cfg : cfgs) {
			String targetTabId = cfg.getAttribute("target");
			if (tabId.equals(targetTabId)) {
				String iconId = cfg.getAttribute("iconId");
				String label = cfg.getAttribute("label");
				// Standard button case
				if ("button".equals(cfg.getName())) {
					String kbDesc = cfg.getAttribute("shortcutKey");
					KeyBinding kb = new KeyBinding(kbDesc);
					Class<IButtonLogic> buttonLogicClass = Activator.getDefault().<IButtonLogic>loadClass(cfg.getContributor().getName(), cfg.getAttribute("logic"));
					IButtonLogic buttonLogic = getContext().newExtensionInstance(buttonLogicClass, AbstractLogicImpl.class, tabLogic);
					IButtonLogic.View buttonView = buttonLogic.getView();
					buttonView.setIcon(iconId);
					buttonView.setDescription(label + " <em>"
							+ kbDesc + "</em>");
					tabLogic.getView().addButton(label, kb.getKey(), kb.isCtrl(), kb.isShift(), kb.isAlt(), buttonView);
				}
				// Download button case
				else {
					Class<IDownloadButtonLogic> buttonLogicClass = Activator.getDefault().<IDownloadButtonLogic>loadClass(cfg.getContributor().getName(), cfg.getAttribute("logic"));
					IDownloadButtonLogic buttonLogic = getContext().newExtensionInstance(buttonLogicClass, AbstractLogicImpl.class, tabLogic);
					IDownloadButtonLogic.View buttonView = buttonLogic.getView();
					buttonView.setIcon(iconId);
					buttonView.setDescription(label);
					tabLogic.getView().addDownloadButton(buttonView);
				}
			}
		}
	}
	
	
	@Override
	public ILogic<?> getParent() {
		return null;
	}

	@Override
	public View getView() {
		return view;
	}
	
	public ILogicContext getContext() {
		return userInjector.getInstance(ILogicContext.class);
	}

	public static void handleError(IRootLogic.View rootView, Throwable error) {
		error.printStackTrace();
		// Building message
		String message = error.getMessage();
		if (message == null || "".equals(message.trim())) {
			message = error.getClass().getSimpleName();
		}
		// Generating details
		String details = null;
		Throwable cause = error;
		while ((cause = cause.getCause()) != null) {
			details = cause.getClass().getSimpleName() + " : " + cause.getMessage() + "\n";
		}
		// FIXME transport the error on the event bus ?
		rootView.showErrorNotification(message, details);
	}

	@Override
	public <T> T injectMembers(T instance) {
		userInjector.injectMembers(instance);
		return instance;
	}

}
