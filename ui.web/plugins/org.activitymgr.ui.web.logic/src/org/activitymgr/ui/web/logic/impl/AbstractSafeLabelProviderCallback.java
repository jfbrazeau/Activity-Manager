package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;

public abstract class AbstractSafeLabelProviderCallback extends AbstractSafeCallback implements ILabelProviderCallback {
	
	public static final String ERROR = "<UNREACHABLE LABEL>";

	public AbstractSafeLabelProviderCallback(ILogic<?> source, IEventBus eventBus) {
		super(source, eventBus);
	}

	@Override
	public String getText() {
		try {
			return unsafeGetText();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return ERROR;
	}

	protected abstract String unsafeGetText() throws Exception;
	
	@Override
	public Icon getIcon() {
		try {
			return unsafeGetIcon();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return Icon.ERROR;
	}

	protected abstract Icon unsafeGetIcon() throws Exception;

}
