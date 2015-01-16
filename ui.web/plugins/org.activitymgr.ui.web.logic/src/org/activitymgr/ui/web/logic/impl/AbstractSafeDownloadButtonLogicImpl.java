package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IDownloadButtonLogic;

public abstract class AbstractSafeDownloadButtonLogicImpl extends AbstractLogicImpl<IDownloadButtonLogic.View> implements IDownloadButtonLogic {
	
	public AbstractSafeDownloadButtonLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);
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
	
	public abstract byte[] unsafeGetContent() throws Exception;

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
	
	public abstract String unsafeGetFileName() throws Exception;
	
}
