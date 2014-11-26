package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;

public abstract class AbstractSafeLabelProviderCallback<TYPE> extends AbstractSafeCallback implements ILabelProviderCallback<TYPE> {
	
	public static final String ERROR = "<UNREACHABLE LABEL>";

	public AbstractSafeLabelProviderCallback(ILogic<?> source, IEventBus eventBus) {
		super(source, eventBus);
	}

	@Override
	public String getText(TYPE object, String propertyId) {
		try {
			return unsafeGetText(object, propertyId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return ERROR;
	}
	
	public abstract String unsafeGetText(TYPE object, String propertyId) throws Exception;

}
