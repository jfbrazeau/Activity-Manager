package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IViewFactory {
	
	IView<?> createView(Class<?> logicType, Object... parameters);

}