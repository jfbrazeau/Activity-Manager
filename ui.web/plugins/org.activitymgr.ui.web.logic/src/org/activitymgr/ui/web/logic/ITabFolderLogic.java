package org.activitymgr.ui.web.logic;

// TODO clear
public interface ITabFolderLogic extends ILogic<ITabFolderLogic.View> {

	void onSelectedTabChanged(String tabId);
	
	public interface View extends ILogic.IView<ITabFolderLogic> {
		
		void addTab(String id, String label, ILogic.IView<?> view);

		void setSelectedTab(String id);
	}

}
