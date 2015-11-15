package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ILogic<VIEW extends IView<?>> {
	
	ILogic<?> getParent();

	VIEW getView();
	
	<T> T injectMembers(T instance);
	
	void dispose();

	interface IView<LOGIC extends ILogic<?>> {
		
		void registerLogic(LOGIC logic);
		
	}

}
