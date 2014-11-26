package org.activitymgr.ui.web.logic;

public interface ITasksTabLogic extends ILogic<ITasksTabLogic.View> {

	String NAME_PROPERTY_ID = ILabelProviderCallback.NAME_PROPERTY_ID;
	String CODE_PROPERTY_ID = "CODE";
	String BUDGET_PROPERTY_ID = "BUDGET";
	String INITIAL_PROPERTY_ID = "INITIAL";
	String COSUMMED_PROPERTY_ID = "CONSUMMED";
	String ETC_PROPERTY_ID = "ETC";
	String DELTA_PROPERTY_ID = "DELTA";
	String COMMENT_PROPERTY_ID = "COMMENT";
	
	public interface View extends ILogic.IView<ITasksTabLogic> {

		void setTreeContentProviderCallback(
				ITreeContentProviderCallback<?> treeContentProviderCallback);
		
	}

}
