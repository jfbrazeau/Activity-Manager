package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;
import org.activitymgr.ui.web.logic.impl.event.Event;
import org.activitymgr.ui.web.logic.impl.event.IEventListener;

public class RootLogicImpl extends AbstractLogicImpl<IRootLogic.View> implements IRootLogic {

	public RootLogicImpl(Context context) {
		super(context, null);
		
		// Event listeners registration
		getEventBus().register(CallbackExceptionEvent.class, new IEventListener() {
			@Override
			public void handle(Event event) {
				// If an error occurs in a view callback, it shows the error to the user
				if (event instanceof CallbackExceptionEvent) {
					handleError(((CallbackExceptionEvent) event).getException());
				}
			}
		});
	}
	

}
