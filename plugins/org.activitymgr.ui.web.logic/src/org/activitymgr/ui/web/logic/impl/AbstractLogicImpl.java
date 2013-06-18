package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.ModelMgr;
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

	public AbstractLogicImpl(AbstractLogicImpl<?> parent) {
		this(parent, parent.getContext());
	}

	public AbstractLogicImpl(RootLogicImpl parent) {
		this(parent, parent.getContext());
	}

	@SuppressWarnings("unchecked")
	private AbstractLogicImpl(ILogic<?> parent, LogicContext context) {
		this.parent = parent;
		this.context = context;
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

	public LogicContext getContext() {
		return context;
	}
	
	protected IEventBus getEventBus() {
		return context != null ? context.getEventBus() : null;
	}
	
	protected ModelMgr getModelMgr() {
		return context != null ? context.getModelMgr() : null;
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

}
