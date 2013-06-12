package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;

/**
 * This event is fired when an error occurs within a callback provided by an
 * {@link ILogic} to an {@link IView}.
 * 
 * @author Jean-Francois Brazeau
 */
public class CallbackExceptionEvent extends AbstractEvent {

	/** The exception that has been thrown */
	private Throwable exception;

	/**
	 * Default constructor.
	 * @param source the event's source.
	 * @param exception the thrown exception.
	 */
	public CallbackExceptionEvent(ILogic<?> source, Throwable exception) {
		super(source);
		this.exception = exception;
	}

	/**
	 * @return the thrown exception.
	 */
	public Throwable getException() {
		return exception;
	}

}
