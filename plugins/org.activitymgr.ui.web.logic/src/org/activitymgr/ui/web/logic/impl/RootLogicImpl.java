package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.impl.event.Event;
import org.activitymgr.ui.web.logic.impl.event.IEventListener;

public class RootLogicImpl extends AbstractLogicImpl<IRootLogic.View> implements IRootLogic {

	public RootLogicImpl(Context context) {
		super(context, null);
		
		// Create authentication logic
		new AuthenticationLogicImpl(context, this);
		getView().showAuthenticationForm();
		
		// Event listeners registration
		getEventBus().register(CallbackExceptionEvent.class, new IEventListener() {
			@Override
			public void handle(Event event) {
				// If an error occurs in a view callback, it shows the error to the user
				handleError(((CallbackExceptionEvent) event).getException());
			}
		});
		getEventBus().register(ConnectedCollaboratorEvent.class, new IEventListener() {
			@Override
			public void handle(Event event) {
				new ContributionsLogicImpl(getContext(), RootLogicImpl.this);
				getView().showContributionsForm();
			}
		});
	}
	

}
