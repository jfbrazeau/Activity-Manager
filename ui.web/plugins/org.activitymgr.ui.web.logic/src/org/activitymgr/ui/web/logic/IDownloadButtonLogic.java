package org.activitymgr.ui.web.logic;

public interface IDownloadButtonLogic extends IButtonLogic<IDownloadButtonLogic.View> {
	
	byte[] getContent();

	String getFileName();

	public interface View extends IButtonLogic.View<IDownloadButtonLogic> {

		void setEnabled(boolean b);

	}

}
