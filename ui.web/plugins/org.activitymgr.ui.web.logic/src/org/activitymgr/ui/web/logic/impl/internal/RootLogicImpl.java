package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Set;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.ITabLogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.impl.event.EventBusImpl;
import org.activitymgr.ui.web.logic.spi.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.spi.ITabFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

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
		eventBus.register(CallbackExceptionEvent.class, new IEventListener<CallbackExceptionEvent>() {
			@Override
			public void handle(CallbackExceptionEvent event) {
				// If an error occurs in a view callback, it shows the error to the user
				handleError(getView(), event.getException());
			}
		});
		eventBus.register(ConnectedCollaboratorEvent.class, new IEventListener<ConnectedCollaboratorEvent>() {
			@Override
			public void handle(ConnectedCollaboratorEvent event) {
				// Create the tab container
				TabFolderLogicImpl tabFolderLogic = new TabFolderLogicImpl(RootLogicImpl.this);
				getView().setContentView(tabFolderLogic.getView());
				
				// Add tabs
				IFeatureAccessManager accessMgr = userInjector.getInstance(IFeatureAccessManager.class);
				Set<ITabFactory> tabFactories = userInjector.getInstance(Key.get(new TypeLiteral<Set<ITabFactory>>() {}));
				for (ITabFactory tabFactory : tabFactories) {
					if (accessMgr.hasAccessToTab(event.getConnectedCollaborator(), tabFactory.getTabId())) {
						ITabLogic<?> tabLogic = tabFactory.create(tabFolderLogic);
						tabFolderLogic.addTab(tabLogic.getLabel(), tabLogic);
					}
				}
				
			}

		});
	}
	
	
	@Override
	public ILogic<?> getParent() {
		return null;
	}

	@Override
	public View getView() {
		return view;
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
