package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicImpl<VIEW extends ILogic.IView> implements ILogic<VIEW> {

	private Object[] EMPTY_ARRAY = new Object[0];
	private ILogicContext context;
	private ILogic<?> parent;

	private VIEW view;

	public AbstractLogicImpl(ILogicContext context) {
		this(context, null);
	}

	public AbstractLogicImpl(ILogic<?> parent) {
		this(parent.getContext(), parent);
	}

	@SuppressWarnings("unchecked")
	private AbstractLogicImpl(ILogicContext context, ILogic<?> parent) {
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

	public ILogicContext getContext() {
		return context;
	}
	
	protected IEventBus getEventBus() {
		return context != null ? context.getEventBus() : null;
	}
	
	public ILogic<?> getParent() {
		return parent;
	}

	protected RootLogicImpl getRoot() {
		ILogic<?> cursor = this;
		while (cursor.getParent() != null) {
			cursor = cursor.getParent();
		}
		return (RootLogicImpl) cursor;
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
