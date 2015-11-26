package org.activitymgr.ui.web.logic;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface ITableCellProviderCallback<ITEM_ID_TYPE> {
	
	IView<?> getCell(ITEM_ID_TYPE itemId, String propertyId);
	
	Integer getColumnWidth(String propertyId);

	String getFooter(String propertyId);

	Collection<String> getPropertyIds();

	Collection<ITEM_ID_TYPE> getRootElements();
	
	boolean contains(ITEM_ID_TYPE element);

}
