package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.ITransactionalWrapperBuilder;
import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;

import com.google.inject.Inject;
import com.google.inject.Injector;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicImpl<VIEW extends ILogic.IView> implements ILogic<VIEW>, ITransactionalWrapperBuilder {

	@Inject
	private Injector injector;
	
	@Inject
	private IEventBus eventBus;
	
	@Inject
	private ITransactionalWrapperBuilder twBuilder;

	@Inject
	private IModelMgr modelMgr;
	
	@Inject
	private ILogicContext context;
	
	private ILogic<?> parent;

	private VIEW view;

	@SuppressWarnings("unchecked")
	protected AbstractLogicImpl(ILogic<?> parent) {
		this.parent = parent;
		
		// Perform injection
		parent.injectMembers(this);
		
		// Create transactional wrapper
		Class<? extends ILogic<?>> iLogicInterface = getILogicInterfaces(getClass());
		// Create the view and bind the logic to it
		Class<VIEW> viewInterface = null;
		for (Class<?> aClass : iLogicInterface.getDeclaredClasses()) {
			if ("View".equals(aClass.getSimpleName()) && aClass.isInterface()) {
				viewInterface = (Class<VIEW>) aClass;
				break;
			}
		}
		if (viewInterface == null) {
			throw new IllegalStateException(iLogicInterface.getSimpleName() + " does not seem to have a nested View interface");
		}
		// Create the view
		view = injector.getInstance(viewInterface);
		
		// Register the logic into the view
		ILogic<VIEW> transactionalWrapper = twBuilder.buildTransactionalWrapper(this, iLogicInterface);
		view.registerLogic(transactionalWrapper);
		
	}

	public VIEW getView() {
		return view;
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
				if (ILogic.class.isAssignableFrom(anInterface)
						&& (result == null || result
								.isAssignableFrom(anInterface))) {
					result = (Class<? extends ILogic<?>>) anInterface;
				}
			};
		}
		return result;
	}

	public <T> T injectMembers(T instance) {
		injector.injectMembers(instance);
		return instance;
	}

	protected IEventBus getEventBus() {
		return eventBus;
	}
	
	protected IModelMgr getModelMgr() {
		return modelMgr;
	}

	protected ILogicContext getContext() {
		return context;
	}

	public <T> T buildTransactionalWrapper(final T wrapped, final Class<?> interfaceToWrapp) {
		return twBuilder.buildTransactionalWrapper(wrapped, interfaceToWrapp);
	}

	@Override
	public void dispose() {
	}

}
