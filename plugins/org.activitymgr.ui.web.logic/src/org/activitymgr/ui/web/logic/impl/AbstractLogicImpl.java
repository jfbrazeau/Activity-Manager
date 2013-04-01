package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.impl.event.EventBus;

@SuppressWarnings("rawtypes")
public abstract class AbstractLogicImpl<VIEW extends IView> implements ILogic<VIEW> {

	private Object[] EMPTY_ARRAY = new Object[0];
	private Context context;
	private AbstractLogicImpl parent;

	private VIEW view;

	@SuppressWarnings("unchecked")
	public AbstractLogicImpl(Context context, AbstractLogicImpl parent) {
		this.context = context;
		this.parent = parent;
		view = (VIEW) context.getViewFactory().createView(getClass(), getViewParameters());
		view.registerLogic(this);
	}

	// TODO Javadoc to be overrided
	protected Object[] getViewParameters() {
		return EMPTY_ARRAY;
	}

	public VIEW getView() {
		return view;
	}

	protected Context getContext() {
		return context;
	}
	
	protected EventBus getEventBus() {
		return context != null ? context.getEventBus() : null;
	}
	
	protected AbstractLogicImpl getParent() {
		return parent;
	}

	protected RootLogicImpl getRoot() {
		return (parent != null) ? parent.getRoot() : (RootLogicImpl) this;
	}

	protected void handleError(Throwable error) {
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
		getRoot().getView().showErrorNotification(message, details);
	}
}
