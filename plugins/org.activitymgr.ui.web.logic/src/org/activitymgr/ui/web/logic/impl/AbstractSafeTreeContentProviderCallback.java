package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.Collections;

import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;

public abstract class AbstractSafeTreeContentProviderCallback extends AbstractSafeCallback implements ITreeContentProviderCallback {
	
	public AbstractSafeTreeContentProviderCallback(AbstractLogicImpl<?> callbackProvider) {
		super(callbackProvider);
	}

	@Override
	public ILabelProviderCallback getLabelProvider(String itemId) {
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
	public Collection<String> getChildren(String itemId) {
		try {
			return unsafeGetChildren(itemId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return Collections.emptyList();
	}

	protected abstract Collection<String> unsafeGetChildren(String itemId) throws Exception;

	@Override
	public Collection<String> rootItemIds() {
		try {
			return unsafeRootItemIds();
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return Collections.emptyList();
	}

	protected abstract Collection<String> unsafeRootItemIds() throws Exception;

	@Override
	public boolean isRoot(String itemId) {
		try {
			return unsafeIsRoot(itemId);
		}
		catch (Throwable t) {
			fireCallbackExceptionEvent(t);
		}
		return false;
	}

	protected abstract boolean unsafeIsRoot(String itemId) throws Exception;

}
