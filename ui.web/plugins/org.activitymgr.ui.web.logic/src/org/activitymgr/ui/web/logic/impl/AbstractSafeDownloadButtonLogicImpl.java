package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.ITabLogic;

public abstract class AbstractSafeDownloadButtonLogicImpl extends AbstractLogicImpl<IDownloadButtonLogic.View> implements IDownloadButtonLogic {
	
	public AbstractSafeDownloadButtonLogicImpl(ITabLogic<?> parent, String label, String iconId) {
		super(parent);
		getView().setIcon(iconId);
		getView().setDescription(label);
	}

	@Override
	public final byte[] getContent() {
		try {
			return unsafeGetContent();
		}
		catch (Throwable t) {
			handleError(t);
			return new byte[0];
		}
	}
	
	protected abstract byte[] unsafeGetContent() throws Exception;

	@Override
	public String getFileName() {
		try {
			return unsafeGetFileName();
		}
		catch (Throwable t) {
			handleError(t);
			return "";
		}
	}
	
	protected abstract String unsafeGetFileName() throws Exception;
	
}
