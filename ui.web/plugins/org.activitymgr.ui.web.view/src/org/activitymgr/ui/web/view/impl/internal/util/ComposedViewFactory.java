package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.HashSet;
import java.util.Set;

import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.IViewFactory;

public class ComposedViewFactory implements IViewFactory {
	
	private Set<IViewFactory> viewFactories = new HashSet<IViewFactory>();

	public void register(IViewFactory factory) {
		viewFactories.add(factory);
	}
	
	@Override
	public IView<?> createView(Class<?> logicType, Object... parameters) {
		for (IViewFactory viewFactory : viewFactories) {
			IView<?> view = viewFactory.createView(logicType, parameters);
			if (view != null) {
				return view;
			}
		}
		throw new IllegalStateException("Unexpected logic type '"
				+ logicType + "'");
	}

}
