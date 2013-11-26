package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.IModelMgr;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicImpl<VIEW extends ILogic.IView> implements ILogic<VIEW> {

	private Object[] EMPTY_ARRAY = new Object[0];
	private LogicContext context;
	private ILogic<?> parent;

	private VIEW view;

	protected AbstractLogicImpl(AbstractLogicImpl<?> parent) {
		this(parent, parent.getContext());
	}

	@SuppressWarnings("unchecked")
	protected AbstractLogicImpl(ILogic<?> parent, LogicContext context) {
		this.parent = parent;
		this.context = context;
		// Create transactional wrapper
		Class<? extends ILogic<?>> iLogicInterface = getILogicInterfaces(getClass());
		ILogic<VIEW> transactionalWrapper = context.buildTransactionalWrapper(this, iLogicInterface);
		// Create the view and bind the logic to it
		view = (VIEW) context.getViewFactory().createView(getClass(), getViewParameters());
		view.registerLogic(transactionalWrapper);
	}

	// TODO Javadoc to be overrided
	protected Object[] getViewParameters() {
		return EMPTY_ARRAY;
	}

	public VIEW getView() {
		return view;
	}

	public LogicContext getContext() {
		return context;
	}
	
	protected IEventBus getEventBus() {
		return context != null ? context.getEventBus() : null;
	}
	
	protected IModelMgr getModelMgr() {
		return context != null ? context.getComponent(IModelMgr.class) : null;
	}
	
	public ILogic<?> getParent() {
		return parent;
	}

	protected IRootLogic getRoot() {
		ILogic<?> cursor = this;
		while (cursor != null && !(cursor instanceof IRootLogic))  {
			cursor = cursor.getParent();
		}
		return (IRootLogic) cursor;
	}

	protected void handleError(Throwable error) {
		RootLogicImpl.handleError(getRoot().getView(), error);
	}

	@SuppressWarnings("unchecked")
	private Class<? extends ILogic<?>> getILogicInterfaces(Class<?> c) {
		Class<? extends ILogic<?>> result = null;
		if (c != Object.class) {
			result = getILogicInterfaces(c.getSuperclass());
			for (Class<?> anInterface : c.getInterfaces()) {
				//System.out.println("  Processing " + anInterface);
				if (ILogic.class.isAssignableFrom(anInterface)
						&& (result == null || result
								.isAssignableFrom(anInterface))) {
					result = (Class<? extends ILogic<?>>) anInterface;
				}
			};
		}
		return result;
	}

}
