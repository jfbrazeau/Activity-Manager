package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;

public abstract class AbstractSafeListContentProviderCallback extends AbstractSafeCallback implements IListContentProviderCallback {
	
	public AbstractSafeListContentProviderCallback(ILogic<?> source, IEventBus eventBus) {
		super(source, eventBus);
	}

	@Override
	public final ILabelProviderCallback getLabelProvider(String itemId) {
		try {
			return unsafeGetLabelProvider(itemId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return new ILabelProviderCallback() {
			@Override
			public Icon getIcon() {
				return Icon.ERROR;
			}
			@Override
			public String getText() {
				return AbstractSafeLabelProviderCallback.ERROR;
			}
		};
	}

	protected abstract ILabelProviderCallback unsafeGetLabelProvider(String itemId) throws Exception;

	@Override
	public final Collection<String> rootItemIds() {
		try {
			return unsafeRootItemIds();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return Collections.emptyList();
	}

	protected abstract Collection<String> unsafeRootItemIds() throws Exception;

}
