package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ILabelLogic extends ILogic<ILabelLogic.View> {
	
	public interface View extends IView<ILabelLogic> {
		
		public static enum Align {
			LEFT, RIGHT
		}
		
		void setLabel(String s);
		
		void setAlign(Align align);

	}

}
