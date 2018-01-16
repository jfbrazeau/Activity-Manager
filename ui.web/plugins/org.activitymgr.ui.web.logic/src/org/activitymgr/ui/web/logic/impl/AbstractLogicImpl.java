package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.IAOPWrappersBuilder;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.IRootLogic;

import com.google.inject.Inject;
import com.google.inject.Injector;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicImpl<VIEW extends ILogic.IView>
 implements
		ILogic<VIEW> {

	@Inject
	private Injector injector;
	
	@Inject
	private IEventBus eventBus;
	
	@Inject
	private IAOPWrappersBuilder aopWrappersBuilder;

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
		Class<ILogic<VIEW>> iLogicInterface = getILogicInterfaces(getClass());
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
		final VIEW realView = injector.getInstance(viewInterface);

		// Wrap it in order to block UI notifications when updates come from the
		// logic
		view = aopWrappersBuilder.buildViewWrapperForLogic(realView,
				viewInterface);

		// Wrap the logic in an AOP container that adds transaction management
		final ILogic<VIEW> transactionalWrapper = aopWrappersBuilder
				.buildLogicWrapperForView(this, iLogicInterface);

		// Register the wrapped logic
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

	@SuppressWarnings("unchecked")
	private Class<ILogic<VIEW>> getILogicInterfaces(Class<?> c) {
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
		return (Class<ILogic<VIEW>>) result;
	}

	protected <LOGIC> LOGIC wrapLogicForView(
			final LOGIC wrapped, final Class<LOGIC> interfaceToWrapp) {
		return aopWrappersBuilder.buildLogicWrapperForView(wrapped,
				interfaceToWrapp);
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

	@Override
	public void dispose() {
	}

	protected void doThrow(Throwable t) {
		if (t instanceof Error) {
			throw (Error) t;
		}
		else if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		else {
			throw new IllegalStateException(t);
		}
	}

}
