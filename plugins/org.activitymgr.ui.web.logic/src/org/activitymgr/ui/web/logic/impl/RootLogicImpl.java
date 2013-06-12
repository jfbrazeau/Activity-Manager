package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;

public class RootLogicImpl extends AbstractLogicImpl<IRootLogic.View> implements IRootLogic {

	public RootLogicImpl(ILogicContext context) {
		super(context);
		
		// Create authentication logic
		getView().show(new AuthenticationLogicImpl(this).getView());
		
		// Event listeners registration
		getEventBus().register(CallbackExceptionEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				// If an error occurs in a view callback, it shows the error to the user
				handleError(((CallbackExceptionEvent) event).getException());
			}
		});
		getEventBus().register(ConnectedCollaboratorEvent.class, new IEventListener() {
			@Override
			public void handle(AbstractEvent event) {
				getView().show(new ContributionsLogicImpl(RootLogicImpl.this).getView());
			}
		});
	}
	

}
