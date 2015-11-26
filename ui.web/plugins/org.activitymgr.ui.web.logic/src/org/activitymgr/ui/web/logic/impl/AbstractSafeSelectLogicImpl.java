package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ISelectFieldLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.event.CallbackExceptionEvent;

public abstract class AbstractSafeSelectLogicImpl<ITEM_ID_TYPE> extends AbstractLogicImpl<ISelectFieldLogic.View<ITEM_ID_TYPE>> implements ISelectFieldLogic<ITEM_ID_TYPE> {

	protected AbstractSafeSelectLogicImpl(ILogic<?> parent, ITableCellProviderCallback<ITEM_ID_TYPE> callback, ITEM_ID_TYPE selectedItemId) {
		super(parent);
		getView().setValuesProviderCallback(callback);
		if (selectedItemId != null) {
			getView().setSelectedItem(selectedItemId);
		}
	}

	@Override
	public void onSelectedItemChanged(ITEM_ID_TYPE newSelectedItemId) {
		try {
			unsafeOnSelectedItemChanged(newSelectedItemId);
		}
		catch (Throwable t) {
			getEventBus().fire(new CallbackExceptionEvent(this, t));
		}
	}

	protected abstract void unsafeOnSelectedItemChanged(ITEM_ID_TYPE newSelectedItemId) throws Exception;

}
