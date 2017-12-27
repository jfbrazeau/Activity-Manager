package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.ITabLogic;

public abstract class AbstractSafeDownloadButtonLogicImpl extends AbstractLogicImpl<IDownloadButtonLogic.View> implements IDownloadButtonLogic {
	
	public AbstractSafeDownloadButtonLogicImpl(ITabLogic<?> parent,
			String label, String description, String iconId) {
		super(parent);
		if (iconId != null) {
			getView().setIcon(iconId);
		} else if (label != null) {
			getView().setLabel(label);
		}
		if (description != null) {
			getView().setDescription(description);
		}
	}

	@Override
	public final byte[] getContent() {
		try {
			return unsafeGetContent();
		}
		catch (Throwable t) {
			doThrow(t);
			return null;
		}
	}
	
	protected abstract byte[] unsafeGetContent() throws Exception;

	@Override
	public String getFileName() {
		try {
			return unsafeGetFileName();
		}
		catch (Throwable t) {
			doThrow(t);
			return null;
		}
	}
	
	protected abstract String unsafeGetFileName() throws Exception;
	
}
