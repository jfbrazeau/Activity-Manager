package org.activitymgr.ui.web.logic;

import java.util.Map;


public interface ISelectFieldLogic<ITEM_ID_TYPE> extends IFieldLogic<ITEM_ID_TYPE, ISelectFieldLogic.View<ITEM_ID_TYPE>> {
	
	public interface View<ITEM_ID_TYPE> extends IFieldLogic.View<ITEM_ID_TYPE, ISelectFieldLogic<ITEM_ID_TYPE>> {
		
		void setItems(Map<ITEM_ID_TYPE, String> items);

		void setWidth(int width);

	}

}
