package org.activitymgr.ui.web.logic;

// TODO clear
public interface ITabFolderLogic extends ILogic<ITabFolderLogic.View> {

	public interface View extends ILogic.IView<ITabFolderLogic> {
		
		void addTab(String label, ILogic.IView<?> view);
	}

}
