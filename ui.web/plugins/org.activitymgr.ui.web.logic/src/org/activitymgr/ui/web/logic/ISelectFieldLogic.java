package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ISelectFieldLogic<ITEM_ID_TYPE> extends ILogic<ISelectFieldLogic.View<ITEM_ID_TYPE>> {
	
	void onSelectedItemChanged(ITEM_ID_TYPE newSelectedItemId);
	
	public interface View<ITEM_ID_TYPE> extends IView<ISelectFieldLogic<ITEM_ID_TYPE>> {
		
		void setSelectedItem(ITEM_ID_TYPE selectedItemId);

		public void setValuesProviderCallback(ITableCellProviderCallback<ITEM_ID_TYPE> callback);

	}

}
