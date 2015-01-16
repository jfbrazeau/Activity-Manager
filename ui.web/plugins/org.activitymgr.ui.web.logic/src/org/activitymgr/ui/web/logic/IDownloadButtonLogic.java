package org.activitymgr.ui.web.logic;

public interface IDownloadButtonLogic extends ILogic<IDownloadButtonLogic.View> {
	
	byte[] getContent();

	String getFileName();

	public interface View extends ILogic.IView<IDownloadButtonLogic> {

		void setIcon(String iconId);
		
		void setDescription(String caption);

	}

}
