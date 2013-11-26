package org.activitymgr.ui.web.logic.impl.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.LogicContext;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.eclipse.core.runtime.IConfigurationElement;

public class RootLogicImpl implements IRootLogic {

	private LogicContext context;
	private IRootLogic.View view;
	
	public RootLogicImpl(IViewFactory viewFactory) {
		// Once the registry is registered, ModelMgr implementation 
		// can be created
		// TODO externalize connection parameters
		// Create the datasource
		String jdbcDriver = "com.mysql.jdbc.Driver";
		String jdbcUrl = "jdbc:mysql://localhost:3306/taskmgr_db";
		String jdbcUser = "taskmgr_user";
		String jdbcPassword = "secret";
		// Context initialization
		try {
			context = new LogicContext(viewFactory, jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}

		// View creation
		view = (IRootLogic.View) viewFactory.createView(getClass());
		view.registerLogic(this);

		// Model manager retrieval
		// Create authentication logic
		getView().setContentView(new AuthenticationLogicImpl(this).getView());
		
		// Event listeners registration
		context.getEventBus().register(CallbackExceptionEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				// If an error occurs in a view callback, it shows the error to the user
				handleError(getView(), ((CallbackExceptionEvent) event).getException());
			}
		});
		context.getEventBus().register(ConnectedCollaboratorEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				// Create the tab container
				TableFolderLogicImpl tabFolderLogic = new TableFolderLogicImpl(RootLogicImpl.this);
				getView().setContentView(tabFolderLogic.getView());
				// Iterate over the provided tabs and create it
				IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.tabLogic");
				for (IConfigurationElement cfg : cfgs) {
					Exception exc = null;
					try {
						Class<AbstractLogicImpl<?>> tabLogicClass = Activator.getDefault().<AbstractLogicImpl<?>>loadClass(cfg.getContributor().getName(), cfg.getAttribute("class"));
						// Tab logic is supposed to have a declared constructor accepting an abstract logic as parent
						Constructor<AbstractLogicImpl<?>> constructor = tabLogicClass.getDeclaredConstructor(AbstractLogicImpl.class);
						AbstractLogicImpl<?> tabLogic = constructor.newInstance(tabFolderLogic);
						tabFolderLogic.addTab(cfg.getAttribute("label"), tabLogic);
					} catch (ClassNotFoundException e) {
						exc = e;
					} catch (NoSuchMethodException e) {
						exc = e;
					} catch (InstantiationException e) {
						exc = e;
					} catch (IllegalAccessException e) {
						exc = e;
					} catch (InvocationTargetException e) {
						exc = e;
					}
					if (exc !=null) {
						System.err.println("Couldn't create a tab logic");
						exc.printStackTrace();
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
	
	public LogicContext getContext() {
		return context;
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

}
