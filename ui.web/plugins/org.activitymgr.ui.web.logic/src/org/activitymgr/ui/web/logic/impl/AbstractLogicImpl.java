package org.activitymgr.ui.web.logic.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.impl.internal.Activator;
import org.activitymgr.ui.web.logic.impl.internal.RootLogicImpl;
import org.eclipse.core.runtime.IConfigurationElement;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicImpl<VIEW extends ILogic.IView> implements ILogic<VIEW> {

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
		Class<VIEW> viewClass = null;
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.viewbinding");
		List<IConfigurationElement> cfgList = new ArrayList<IConfigurationElement>(Arrays.asList(cfgs));
		try {
			for (IConfigurationElement cfg : cfgList) {
				if (iLogicInterface.getName().equals(cfg.getAttribute("logic"))) {
					viewClass = Activator.getDefault().<VIEW>loadClass(cfg.getContributor().getName(), cfg.getAttribute("view"));
					break;
				}
			}
			if (viewClass == null) {
				throw new IllegalStateException("Unknown view implementation for " + iLogicInterface.getName());
			}
			Constructor<VIEW> viewConsructor = viewClass.getDeclaredConstructor(context.getViewDescriptor().getConstructorArgTypes());
			view = viewConsructor.newInstance(context.getViewDescriptor().getConstructorArgs());
			view.registerLogic(transactionalWrapper);
		}
		catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
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
