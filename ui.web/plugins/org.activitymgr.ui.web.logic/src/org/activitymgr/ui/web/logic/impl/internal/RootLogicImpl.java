package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.core.ModelMgr;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.logic.impl.LogicContext;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.impl.event.EventBusImpl;

public class RootLogicImpl implements IRootLogic {

	private LogicContext context;
	private IRootLogic.View view;
	
	public RootLogicImpl(IViewFactory viewFactory) {
		// View creation
		view = (IRootLogic.View) viewFactory.createView(getClass());
		view.registerLogic(this);

		// Model manager retrieval
		ModelMgr modelMgr = Activator.getDefault().getModelMgr();
		if (modelMgr == null) {
			getView().showErrorNotification("Database initialization failure", "See logs for more details");
		}
		else {
			// Context initialization
			context = new LogicContext(viewFactory, new EventBusImpl(), modelMgr);
			
			// Create authentication logic
			getView().show(new AuthenticationLogicImpl(this).getView());
			
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
					getView().show(new ContributionsLogicImpl(RootLogicImpl.this).getView());
				}
			});
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
