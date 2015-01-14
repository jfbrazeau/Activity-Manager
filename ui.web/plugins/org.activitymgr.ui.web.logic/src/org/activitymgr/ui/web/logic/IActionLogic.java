package org.activitymgr.ui.web.logic;

public interface IActionLogic<VIEW extends IActionLogic.View<?>> extends ILogic<VIEW> {
	
	void onAction(String actionId);
	
	public interface View<LOGIC extends IActionLogic<?>> extends IView<LOGIC> {
		
		void addAction(String id, String label, String string, String iconId,
				char key, boolean ctrl, boolean shift, boolean alt);

	}

}
